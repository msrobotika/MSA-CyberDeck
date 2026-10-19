#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"
#include "driver/spi_master.h"
#include "driver/gpio.h"
#include "esp_lcd_panel_io.h"
#include "esp_lcd_panel_ops.h"
#include "esp_lcd_panel_vendor.h"
#include "esp_heap_caps.h"
#include "esp_timer.h"
#include "ui_fonts.h"
#include "sentinel.h"
#define BG 0x090F1A
#define CARD 0x111D2E
#define EDGE 0x20394D
#define WHITE 0xE9F0FC
#define DIM 0xB4C2D8
#define CYAN 0x71E5F4
#define GREEN 0x8EE7BD
#define MAGENTA 0xB6AAFF
#define AMBER 0xFFB02E
#define RED 0xFF315B
static esp_lcd_panel_handle_t panel;static SemaphoreHandle_t flushed,capture_done;static uint16_t *pixels;static int width=170,height=320;static volatile int page,requested_profile,applied_profile=-1;static volatile bool requested_invert=true,applied_invert=true,capture_requested;static volatile unsigned frames,button_presses,button_holds;static volatile uint32_t manual_until;static uint32_t last_rotation;static int64_t frame_ms;static sentinel_snapshot_t view;
static const char *titles[]={"RADAR WI-FI","REDES","FICHA DE RED","OBSERVACIONES","ENLACE / DATOS","FIJAR REFERENCIA"};
static uint32_t millis(void){return (uint32_t)(esp_timer_get_time()/1000);}static bool manual(uint32_t now){return manual_until&&(int32_t)(manual_until-now)>0;}static uint16_t rgb565(uint32_t c){return __builtin_bswap16(((c>>8)&0xf800)|((c>>5)&0x7e0)|((c>>3)&31));}
static void pixel(int x,int y,uint32_t c){if(x>=0&&x<width&&y>=0&&y<height)pixels[y*width+x]=rgb565(c);}static void rect(int x,int y,int w,int h,uint32_t c){int l=x<0?0:x,r=x+w>width?width:x+w,t=y<0?0:y,b=y+h>height?height:y+h;uint16_t color=rgb565(c);for(int yy=t;yy<b;yy++)for(int xx=l;xx<r;xx++)pixels[yy*width+xx]=color;}
static void line(int x0,int y0,int x1,int y1,uint32_t c){int dx=abs(x1-x0),sx=x0<x1?1:-1,dy=-abs(y1-y0),sy=y0<y1?1:-1,e=dx+dy;for(;;){pixel(x0,y0,c);if(x0==x1&&y0==y1)break;int q=2*e;if(q>=dy){e+=dy;x0+=sx;}if(q<=dx){e+=dx;y0+=sy;}}}static void outline(int x,int y,int w,int h,uint32_t c){line(x,y,x+w-1,y,c);line(x,y+h-1,x+w-1,y+h-1,c);line(x,y,x,y+h-1,c);line(x+w-1,y,x+w-1,y+h-1,c);}
static void alpha(int x,int y,uint32_t c,uint8_t a){if(!a||x<0||x>=width||y<0||y>=height)return;uint16_t old=__builtin_bswap16(pixels[y*width+x]);int r=((old>>11)&31)*255/31,g=((old>>5)&63)*255/63,b=(old&31)*255/31;r=(r*(255-a)+((c>>16)&255)*a)/255;g=(g*(255-a)+((c>>8)&255)*a)/255;b=(b*(255-a)+(c&255)*a)/255;pixel(x,y,(r<<16)|(g<<8)|b);}
static int tw(const char *s,const ui_font_t *f){int n=0;for(;*s;s++){unsigned c=(unsigned char)*s;if(c<32||c>126)c='?';n+=f->glyph[c-32].width;}return n;}static void text(int x,int y,const char *s,const ui_font_t *f,uint32_t color){for(;*s;s++){unsigned c=(unsigned char)*s;if(c<32||c>126)c='?';const glyph_t *g=&f->glyph[c-32];for(int yy=0;yy<f->height;yy++)for(int xx=0;xx<g->width;xx++)alpha(x+xx,y+yy,color,f->data[g->offset+yy*g->width+xx]);x+=g->width;if(x>=width)break;}}static void centered(int y,const char*s,const ui_font_t*f,uint32_t c){text((width-tw(s,f))/2,y,s,f,c);}static void clipped(int x,int y,int available,const char*s,const ui_font_t*f,uint32_t c){char small[48];strlcpy(small,s,sizeof(small));if(tw(small,f)>available){size_t n=strlen(small);while(n&&tw(small,f)+tw("..",f)>available)small[--n]=0;strlcat(small,"..",sizeof(small));}text(x,y,small,f,c);}
static void robot(int x,int y){
 line(x+2,y+3,x+8,y,WHITE);line(x+8,y,x+14,y+3,WHITE);
 line(x+2,y+3,x+3,y+12,DIM);line(x+14,y+3,x+13,y+12,DIM);
 line(x+3,y+12,x+8,y+16,DIM);line(x+8,y+16,x+13,y+12,DIM);
 line(x+4,y+3,x+8,y+6,MAGENTA);line(x+12,y+3,x+8,y+6,MAGENTA);
 uint32_t eye=frame_ms/900%2?CYAN:GREEN;
 rect(x+4,y+6,3,2,eye);rect(x+10,y+6,3,2,eye);
 line(x+6,y+12,x+10,y+12,CYAN);
}
static void header(void){
 robot(8,3);text(31,3,"CIBERDECK",&font13,WHITE);
 text(8,22,titles[page],&font13,CYAN);
 text(width-61,3,view.running?"PASIVO":"PAUSA",&font11,view.running?GREEN:AMBER);
 if(page<PAGE_COUNT)text(width-61,23,"BOOT >",&font11,DIM);
 line(8,39,width-9,39,EDGE);
 if(view.last_deauth&&view.uptime-view.last_deauth<8&&frame_ms/250%2==0)outline(1,1,width-2,height-2,RED);
}
static void footer(void){
 int y=height-23;line(8,y,width-9,y,EDGE);
 const char *labels[]={"RADAR","REDES","FICHA","AVISOS","USB"};
 if(page<PAGE_COUNT){int step=(width-16)/PAGE_COUNT;for(int i=0;i<PAGE_COUNT;i++){
  int x=8+i*step;if(i==page)rect(x,y+3,step-2,19,EDGE);
  text(x+(step-2-tw(labels[i],&font11))/2,y+5,labels[i],&font11,i==page?CYAN:DIM);
 }}else text(8,y+5,"1x: cancelar | 2s: confirmar",&font11,AMBER);
}
static unsigned channel_aps(unsigned c){unsigned n=0;for(unsigned i=0;i<view.count;i++)if(view.aps[i].channel==c)n++;return n;}
/* True one-second receive rates, maintained on every page; no decorative samples. */
static uint32_t rates[36],rate_count,rate_pos,rate_last_ms,rate_last_frames;
static void sample_rate(uint32_t now){
 if(!rate_last_ms || view.frames<rate_last_frames){rate_count=rate_pos=0;rate_last_ms=now;rate_last_frames=view.frames;return;}
 uint32_t elapsed=now-rate_last_ms;if(elapsed<1000)return;
 rates[rate_pos]=(uint32_t)((uint64_t)(view.frames-rate_last_frames)*1000/elapsed);
 rate_pos=(rate_pos+1)%36;if(rate_count<36)rate_count++;
 rate_last_ms=now;rate_last_frames=view.frames;
}
static void signal_bars(int x,int y,int rssi,uint32_t color){
 int level=(rssi+100)/14;if(level<0)level=0;if(level>5)level=5;
 for(int i=0;i<5;i++)rect(x+i*5,y-(i+1)*2,3,(i+1)*2,i<level?color:EDGE);
}
static void arc(int cx,int cy,int radius,float from,float to,uint32_t color){
 for(int i=0;i<50;i++){float a=from+(to-from)*i/50,b=from+(to-from)*(i+1)/50;
  for(int r=radius-2;r<=radius;r++)line(cx+(int)(cosf(a)*r),cy+(int)(sinf(a)*r),cx+(int)(cosf(b)*r),cy+(int)(sinf(b)*r),color);
 }
}
static void spectrum(void){
 const int compact=height<150; const int cx=compact?46:64,cy=compact?79:91,rad=compact?29:42; const int sx=compact?88:126,base=compact?94:116,step=compact?10:13;
 for(int ring=1;ring<=3;ring++)for(int step=0;step<48;step++){
  float a=step*6.283185f/48,b=(step+1)*6.283185f/48;int r=rad*ring/3;
  line(cx+(int)(cosf(a)*r),cy+(int)(sinf(a)*r),cx+(int)(cosf(b)*r),cy+(int)(sinf(b)*r),EDGE);
 }
 line(cx-rad,cy,cx+rad,cy,EDGE);line(cx,cy-rad,cx,cy+rad,EDGE);
 float sweep=-1.570796f+(view.channel-1)*6.283185f/13;
 line(cx,cy,cx+(int)(cosf(sweep)*rad),cy+(int)(sinf(sweep)*rad),CYAN);
 for(unsigned i=0;i<view.count;i++){
  if(view.aps[i].channel<1||view.aps[i].channel>13)continue;
  float r=(-view.aps[i].rssi-30)/70.0f;if(r<0)r=0;if(r>1)r=1;
  float a=-1.570796f+(view.aps[i].channel-1)*6.283185f/13;int x=cx+(int)(cosf(a)*r*rad),y=cy+(int)(sinf(a)*r*rad);
  rect(x-1,y-1,3,3,GREEN);
 }
 char value[48];snprintf(value,sizeof(value),"%u REDES | CH %u",view.count,view.channel);text(sx,48,value,&font13,WHITE);
 text(sx,66,compact?"CANAL / RSSI":"CANAL / RSSI, NO DIRECCION",&font11,DIM);
 unsigned max=1;for(unsigned c=1;c<=13;c++){unsigned n=channel_aps(c);if(n>max)max=n;}
 for(unsigned c=1;c<=13;c++){int x=sx+(c-1)*step;unsigned n=channel_aps(c);int h=n*(compact?15:30)/max;
  rect(x,base-h,compact?7:9,h,c==view.channel?CYAN:GREEN);if(!compact||c%2==1){char label[4];snprintf(label,sizeof(label),"%u",c);text(x,base+4,label,&font11,DIM);}
 }
}
static void networks(void){
 if(!view.count){centered(57,"ESCUCHANDO",&font19,CYAN);centered(86,"Sin redes observadas aun",&font11,DIM);return;}
 unsigned rows=height<150?2:3,start=view.selected/rows*rows;
 for(unsigned row=0;row<rows&&start+row<view.count;row++){
  unsigned id=start+row;ap_t*a=&view.aps[id];int y=46+row*31;
  uint32_t color=a->security==SEC_OPEN?AMBER:CYAN;
  rect(8,y,width-16,28,CARD);rect(8,y,3,28,id==view.selected?CYAN:EDGE);
  clipped(16,y,width-82,a->ssid[0]?a->ssid:"SSID oculto",&font13,WHITE);
  char value[48];snprintf(value,sizeof(value),"%d",a->rssi);text(width-48,y,value,&font13,color);
  snprintf(value,sizeof(value),"%02u/%02u  CH %u  %s",id+1,view.count,a->channel,sentinel_security(a->security));
  clipped(16,y+15,width-60,value,&font11,DIM);signal_bars(width-41,y+25,a->rssi,color);
 }
}
static void fingerprint(void){
 if(!view.count){centered(78,"SIN RED ELEGIDA",&font19,DIM);return;}
 ap_t*a=&view.aps[view.selected];char address[18],value[48];sentinel_address(a->bssid,address);
 clipped(9,45,width-18,a->ssid[0]?a->ssid:"SSID oculto",&font13,WHITE);
 float level=(a->rssi+100)/70.0f;if(level<0)level=0;if(level>1)level=1;
 arc(34,84,22,2.35f,7.07f,EDGE);if(level>0)arc(34,84,22,2.35f,2.35f+4.72f*level,CYAN);
 snprintf(value,sizeof(value),"%d",a->rssi);text(34-tw(value,&font13)/2,73,value,&font13,WHITE);text(23,89,"dBm",&font11,DIM);
 text(68,66,"CANAL",&font11,DIM);snprintf(value,sizeof(value),"%u",a->channel);text(68,80,value,&font19,CYAN);
 text(117,66,"SEGURIDAD",&font11,DIM);text(117,80,sentinel_security(a->security),&font13,a->security==SEC_OPEN?AMBER:GREEN);
 clipped(68,98,width-76,address,&font11,DIM);
 if(height>=150)text(9,122,view.baseline_set&&!strcmp(a->ssid,view.baseline_ssid)?"SSID VIGILADO":"2s: fijar referencia propia",&font11,GREEN);
}
static void alerts(void){
 const char*labels[]={"CAMBIOS","DEAUTH","DISASSOC"};uint32_t counts[]={view.security_mismatches,view.deauth,view.disassoc};
 int step=(width-16)/3;char value[32];
 for(int i=0;i<3;i++){int x=8+i*step;uint32_t color=counts[i]?(i==0?RED:AMBER):DIM;
  rect(x,46,step-4,47,CARD);rect(x,46,step-4,2,color);
  snprintf(value,sizeof(value),"%lu",(unsigned long)counts[i]);clipped(x+7,51,step-15,value,&font19,color);
  text(x+7,77,labels[i],&font11,DIM);
 }
 clipped(9,98,width-18,view.baseline_set?"Referencia activa | no prueba ataques":"Sin referencia: no compara SSID",&font11,view.baseline_set?CYAN:AMBER);
 if(height>=150)text(9,120,"Tramas observadas, no intrusiones",&font11,DIM);
}
static void evidence(void){
 char value[48];int split=width/2;
 text(9,44,"TRAMAS RECIBIDAS",&font11,DIM);snprintf(value,sizeof(value),"%lu",(unsigned long)view.frames);clipped(9,58,split-16,value,&font19,WHITE);
 text(split+8,44,"SERIE 115200",&font11,CYAN);
 if(rate_count){snprintf(value,sizeof(value),"%lu tramas/s",(unsigned long)rates[(rate_pos+35)%36]);clipped(split+8,62,split-16,value,&font11,GREEN);}else text(split+8,62,"Midiendo ritmo...",&font11,DIM);
 const int x=9,y=83,w=width-18,h=25;rect(x,y,w,h,CARD);
 for(int i=1;i<4;i++)line(x,y+i*h/4,x+w-1,y+i*h/4,EDGE);
 if(rate_count>1){uint32_t max=1;for(unsigned i=0;i<rate_count;i++)if(rates[i]>max)max=rates[i];
  for(unsigned i=1;i<rate_count;i++){unsigned first=(rate_pos+36-rate_count)%36;int x0=x+(i-1)*(w-1)/35,x1=x+i*(w-1)/35;int y0=y+h-2-(uint64_t)rates[(first+i-1)%36]*(h-3)/max,y1=y+h-2-(uint64_t)rates[(first+i)%36]*(h-3)/max;line(x0,y0,x1,y1,GREEN);}
 }
 if(height>=150)text(9,120,"Recepcion real | historial de 36 s",&font11,DIM);
}
static void confirm(void){
 centered(46,"FIJAR REFERENCIA PROPIA",&font13,AMBER);
 if(view.count)clipped(12,66,width-24,view.aps[view.selected].ssid,&font13,WHITE);
 centered(85,"Guarda SSID, BSSID, canal y seguridad.",&font11,DIM);
 centered(96,"No guarda contrasenas.",&font11,DIM);
}
static void apply_geometry(void){if(applied_profile==requested_profile&&applied_invert==requested_invert)return;applied_profile=requested_profile;applied_invert=requested_invert;width=applied_profile?240:320;height=applied_profile?135:170;esp_lcd_panel_swap_xy(panel,true);esp_lcd_panel_mirror(panel,true,false);esp_lcd_panel_set_gap(panel,applied_profile?40:0,applied_profile?53:35);esp_lcd_panel_invert_color(panel,applied_invert);}static bool done(esp_lcd_panel_io_handle_t io,esp_lcd_panel_io_event_data_t*e,void*c){BaseType_t wake=pdFALSE;xSemaphoreGiveFromISR(flushed,&wake);return wake==pdTRUE;}
static void short_press(void){page=page==5?2:(page+1)%PAGE_COUNT;manual_until=millis()+60000;}static void double_press(void){if(page==0)sentinel_clear();else if(page==1||page==2)sentinel_next();manual_until=millis()+60000;}static void hold_press(void){manual_until=millis()+60000;if(page==0)sentinel_set_running(!view.running);else if(page==1)page=2;else if(page==2&&view.count)page=5;else if(page==3)sentinel_forget_baseline();else if(page==5){sentinel_baseline_selected();page=3;}}
static void button_task(void*unused){bool stable=true,raw=true;uint32_t changed=0,down=0,last_up=0;bool held=false,pending=false;while(1){uint32_t now=millis();bool level=gpio_get_level(0);if(level!=raw){raw=level;changed=now;}if(level!=stable&&now-changed>=30){stable=level;if(!stable){down=now;held=false;}else{button_presses++;if(held){button_holds++;pending=false;}else if(pending&&now-last_up<330){pending=false;double_press();}else{pending=true;last_up=now;}}}if(!stable&&!held&&now-down>=1600){held=true;hold_press();}if(pending&&now-last_up>=330){pending=false;short_press();}vTaskDelay(pdMS_TO_TICKS(10));}}
static void screen_task(void*unused){while(1){frame_ms=esp_timer_get_time()/1000;uint32_t now=millis();sentinel_snapshot(&view);sample_rate(now);if(manual(now)||page==5)last_rotation=now;else if(now-last_rotation>12000){page=(page+1)%PAGE_COUNT;last_rotation=now;}apply_geometry();rect(0,0,width,height,BG);header();if(page==0)spectrum();else if(page==1)networks();else if(page==2)fingerprint();else if(page==3)alerts();else if(page==4)evidence();else confirm();footer();while(xSemaphoreTake(flushed,0)==pdTRUE){}if(esp_lcd_panel_draw_bitmap(panel,0,0,width,height,pixels)==ESP_OK&&xSemaphoreTake(flushed,pdMS_TO_TICKS(1000))==pdTRUE)frames++;if(capture_requested){capture_requested=false;serial_blob((uint8_t*)pixels,width*height*2,width,height);xSemaphoreGive(capture_done);}vTaskDelay(pdMS_TO_TICKS(90));}}
void screen_start(int profile,bool invert){requested_profile=profile;requested_invert=invert;flushed=xSemaphoreCreateBinary();capture_done=xSemaphoreCreateBinary();pixels=heap_caps_malloc(170*320*2,MALLOC_CAP_DMA);assert(flushed&&capture_done&&pixels);spi_bus_config_t bus={.mosi_io_num=23,.miso_io_num=-1,.sclk_io_num=18,.quadwp_io_num=-1,.quadhd_io_num=-1,.max_transfer_sz=170*320*2};ESP_ERROR_CHECK(spi_bus_initialize(SPI2_HOST,&bus,SPI_DMA_CH_AUTO));esp_lcd_panel_io_handle_t io;esp_lcd_panel_io_spi_config_t cfg={.cs_gpio_num=15,.dc_gpio_num=2,.spi_mode=0,.pclk_hz=16000000,.trans_queue_depth=1,.lcd_cmd_bits=8,.lcd_param_bits=8,.on_color_trans_done=done};ESP_ERROR_CHECK(esp_lcd_new_panel_io_spi((esp_lcd_spi_bus_handle_t)SPI2_HOST,&cfg,&io));esp_lcd_panel_dev_config_t dev={.reset_gpio_num=4,.rgb_ele_order=LCD_RGB_ELEMENT_ORDER_RGB,.bits_per_pixel=16};ESP_ERROR_CHECK(esp_lcd_new_panel_st7789(io,&dev,&panel));ESP_ERROR_CHECK(esp_lcd_panel_reset(panel));ESP_ERROR_CHECK(esp_lcd_panel_init(panel));apply_geometry();ESP_ERROR_CHECK(esp_lcd_panel_disp_on_off(panel,true));gpio_config_t led={.pin_bit_mask=1ULL<<32,.mode=GPIO_MODE_OUTPUT};ESP_ERROR_CHECK(gpio_config(&led));gpio_set_level(32,1);gpio_config_t button={.pin_bit_mask=1ULL,.mode=GPIO_MODE_INPUT,.pull_up_en=GPIO_PULLUP_ENABLE};ESP_ERROR_CHECK(gpio_config(&button));assert(xTaskCreate(screen_task,"sentinel_ui",6144,NULL,3,NULL)==pdPASS);assert(xTaskCreate(button_task,"sentinel_button",3072,NULL,3,NULL)==pdPASS);}
void screen_page(int value){if(value>=0&&value<PAGE_COUNT){page=value;manual_until=millis()+60000;}}void screen_profile(int p,bool i){requested_profile=p;requested_invert=i;}bool screen_capture(void){while(xSemaphoreTake(capture_done,0)==pdTRUE){}capture_requested=true;return xSemaphoreTake(capture_done,pdMS_TO_TICKS(20000))==pdTRUE;}unsigned screen_frames(void){return frames;}unsigned screen_button_presses(void){return button_presses;}unsigned screen_button_holds(void){return button_holds;}int screen_button_level(void){return gpio_get_level(0);}int screen_current_page(void){return page;}int screen_current_profile(void){return applied_profile;}bool screen_current_invert(void){return applied_invert;}




