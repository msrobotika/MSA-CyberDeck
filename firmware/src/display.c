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
#define BG 0x050A12
#define CARD 0x0C1A2A
#define EDGE 0x173750
#define WHITE 0xE8F8FF
#define DIM 0x7895A8
#define CYAN 0x22E2FF
#define GREEN 0x2BFF9B
#define MAGENTA 0xFF42D0
#define AMBER 0xFFB02E
#define RED 0xFF315B
static esp_lcd_panel_handle_t panel;static SemaphoreHandle_t flushed,capture_done;static uint16_t *pixels;static int width=170,height=320;static volatile int page,requested_profile,applied_profile=-1;static volatile bool requested_invert=true,applied_invert=true,capture_requested;static volatile unsigned frames,button_presses,button_holds;static volatile uint32_t manual_until;static uint32_t last_rotation;static int64_t frame_ms;static sentinel_snapshot_t view;
static const char *titles[]={"RADAR / CANALES","REDES","HUELLA","ALERTAS","EVIDENCIAS","FIJAR REFERENCIA"};
static uint32_t millis(void){return (uint32_t)(esp_timer_get_time()/1000);}static bool manual(uint32_t now){return manual_until&&(int32_t)(manual_until-now)>0;}static uint16_t rgb565(uint32_t c){return __builtin_bswap16(((c>>8)&0xf800)|((c>>5)&0x7e0)|((c>>3)&31));}
static void pixel(int x,int y,uint32_t c){if(x>=0&&x<width&&y>=0&&y<height)pixels[y*width+x]=rgb565(c);}static void rect(int x,int y,int w,int h,uint32_t c){int l=x<0?0:x,r=x+w>width?width:x+w,t=y<0?0:y,b=y+h>height?height:y+h;uint16_t color=rgb565(c);for(int yy=t;yy<b;yy++)for(int xx=l;xx<r;xx++)pixels[yy*width+xx]=color;}
static void line(int x0,int y0,int x1,int y1,uint32_t c){int dx=abs(x1-x0),sx=x0<x1?1:-1,dy=-abs(y1-y0),sy=y0<y1?1:-1,e=dx+dy;for(;;){pixel(x0,y0,c);if(x0==x1&&y0==y1)break;int q=2*e;if(q>=dy){e+=dy;x0+=sx;}if(q<=dx){e+=dx;y0+=sy;}}}static void outline(int x,int y,int w,int h,uint32_t c){line(x,y,x+w-1,y,c);line(x,y+h-1,x+w-1,y+h-1,c);line(x,y,x,y+h-1,c);line(x+w-1,y,x+w-1,y+h-1,c);}
static void alpha(int x,int y,uint32_t c,uint8_t a){if(!a||x<0||x>=width||y<0||y>=height)return;uint16_t old=__builtin_bswap16(pixels[y*width+x]);int r=((old>>11)&31)*255/31,g=((old>>5)&63)*255/63,b=(old&31)*255/31;r=(r*(255-a)+((c>>16)&255)*a)/255;g=(g*(255-a)+((c>>8)&255)*a)/255;b=(b*(255-a)+(c&255)*a)/255;pixel(x,y,(r<<16)|(g<<8)|b);}
static int tw(const char *s,const ui_font_t *f){int n=0;for(;*s;s++){unsigned c=(unsigned char)*s;if(c<32||c>126)c='?';n+=f->glyph[c-32].width;}return n;}static void text(int x,int y,const char *s,const ui_font_t *f,uint32_t color){for(;*s;s++){unsigned c=(unsigned char)*s;if(c<32||c>126)c='?';const glyph_t *g=&f->glyph[c-32];for(int yy=0;yy<f->height;yy++)for(int xx=0;xx<g->width;xx++)alpha(x+xx,y+yy,color,f->data[g->offset+yy*g->width+xx]);x+=g->width;if(x>=width)break;}}static void centered(int y,const char*s,const ui_font_t*f,uint32_t c){text((width-tw(s,f))/2,y,s,f,c);}static void clipped(int x,int y,int available,const char*s,const ui_font_t*f,uint32_t c){char small[48];strlcpy(small,s,sizeof(small));if(tw(small,f)>available){size_t n=strlen(small);while(n&&tw(small,f)+tw("..",f)>available)small[--n]=0;strlcat(small,"..",sizeof(small));}text(x,y,small,f,c);}
static void header(void){
 text(8,2,"MSA_CYBERDECK",&font11,DIM);
 text(8,16,titles[page],&font19,WHITE);
 text(width-90,3,view.running?"PASIVO ACTIVO":"EN PAUSA",&font11,view.running?GREEN:AMBER);
 line(8,39,width-9,39,EDGE);
 rect(8,39,10+(frame_ms/35)%(width-26),1,CYAN);
 if(view.last_deauth&&view.uptime-view.last_deauth<8&&frame_ms/250%2==0)outline(1,1,width-2,height-2,RED);
}
static void footer(void){
 int y=height-23;line(8,y,width-9,y,EDGE);
 const char *hint=page==0?"1x: vista  |  2s: pausa":page==1?"1x: vista  2x: red  2s: huella":page==2?"1x: vista  2x: red  2s: referencia":page==3?"1x: vista  2s: olvidar ref.":page==4?"1x: vista  |  USB: CyberDeck":"1x: cancelar  |  2s: confirmar";
 text(8,height-17,hint,&font11,DIM);
 if(page<PAGE_COUNT)for(int i=0;i<PAGE_COUNT;i++)rect(width-36+i*6,height-5,4,2,i==page?CYAN:EDGE);
}
static unsigned channel_aps(unsigned c){unsigned n=0;for(unsigned i=0;i<view.count;i++)if(view.aps[i].channel==c)n++;return n;}
static void spectrum(void){
 const int cx=64,cy=91,rad=42;
 for(int ring=1;ring<=3;ring++)for(int step=0;step<48;step++){
  float a=step*6.283185f/48,b=(step+1)*6.283185f/48;int r=rad*ring/3;
  line(cx+(int)(cosf(a)*r),cy+(int)(sinf(a)*r),cx+(int)(cosf(b)*r),cy+(int)(sinf(b)*r),EDGE);
 }
 line(cx-rad,cy,cx+rad,cy,EDGE);line(cx,cy-rad,cx,cy+rad,EDGE);
 float sweep=(frame_ms%4000)*6.283185f/4000;
 line(cx,cy,cx+(int)(cosf(sweep)*rad),cy+(int)(sinf(sweep)*rad),CYAN);
 for(unsigned i=0;i<view.count;i++){
  float r=(-view.aps[i].rssi-30)/70.0f;if(r<0.12f)r=0.12f;if(r>1)r=1;
  float a=i*2.399963f;int x=cx+(int)(cosf(a)*r*rad),y=cy+(int)(sinf(a)*r*rad);
  rect(x-1,y-1,3,3,GREEN);
 }
 char value[48];snprintf(value,sizeof(value),"%u REDES | CH %u",view.count,view.channel);text(126,48,value,&font13,WHITE);
 text(126,68,"RSSI / SIN DIRECCION",&font11,DIM);
 unsigned max=1;for(unsigned c=1;c<=13;c++){unsigned n=channel_aps(c);if(n>max)max=n;}
 for(unsigned c=1;c<=13;c++){int x=126+(c-1)*13;unsigned n=channel_aps(c);int h=n*30/max;
  rect(x,116-h,9,h,c==view.channel?CYAN:GREEN);char label[4];snprintf(label,sizeof(label),"%u",c);text(x,120,label,&font11,DIM);
 }
}
static void networks(void){
 if(!view.count){centered(66,"ESCUCHANDO REDES",&font19,CYAN);centered(102,"Sin transmitir paquetes",&font11,DIM);return;}
 unsigned rows=3,start=view.selected/rows*rows;
 for(unsigned row=0;row<rows&&start+row<view.count;row++){
  unsigned id=start+row;ap_t*a=&view.aps[id];int y=46+row*31;
  rect(8,y,width-16,28,CARD);if(id==view.selected)outline(8,y,width-16,28,CYAN);
  char value[48];snprintf(value,sizeof(value),"#%02u %s",id,a->ssid[0]?a->ssid:"SSID oculto");
  clipped(14,y+1,165,value,&font13,WHITE);
  snprintf(value,sizeof(value),"CH%u %d %s",a->channel,a->rssi,sentinel_security(a->security));
  clipped(181,y+5,width-192,value,&font11,a->security==SEC_OPEN?AMBER:GREEN);
 }
}
static void fingerprint(void){
 if(!view.count){centered(78,"SIN RED ELEGIDA",&font19,DIM);return;}
 ap_t*a=&view.aps[view.selected];char address[18],value[48];sentinel_address(a->bssid,address);
 clipped(9,47,width-18,a->ssid[0]?a->ssid:"SSID oculto",&font19,WHITE);
 text(9,72,address,&font13,CYAN);
 snprintf(value,sizeof(value),"CH %u | %d dBm | %s",a->channel,a->rssi,sentinel_security(a->security));
 text(9,94,value,&font13,DIM);
 text(9,119,view.baseline_set&&!strcmp(a->ssid,view.baseline_ssid)?"SSID VIGILADO":"2s: fijar referencia propia",&font11,GREEN);
}
static void alerts(void){
 char value[48];snprintf(value,sizeof(value),"%lu",(unsigned long)view.security_mismatches);
 text(10,47,"CAMBIOS SEGURIDAD",&font11,DIM);text(10,64,value,&font26,view.security_mismatches?RED:GREEN);
 text(100,73,view.security_mismatches?"REVISAR":"SIN AVISOS",&font19,view.security_mismatches?RED:GREEN);
 snprintf(value,sizeof(value),"%lu deauth | %lu disassoc",(unsigned long)view.deauth,(unsigned long)view.disassoc);
 text(10,104,value,&font13,view.deauth?AMBER:DIM);
 text(10,126,"Un aviso no demuestra un ataque",&font11,DIM);
}
static void evidence(void){
 char value[48];snprintf(value,sizeof(value),"%lu tramas | %u redes",(unsigned long)view.frames,view.count);text(10,49,value,&font19,WHITE);
 snprintf(value,sizeof(value),"%lu beacons | %lu descartes",(unsigned long)view.beacons,(unsigned long)view.dropped);text(10,78,value,&font13,DIM);
 text(10,102,"USB 115200 | CH340 | APP ANDROID",&font11,CYAN);
 text(10,124,"Sin claves / sin inyeccion / datos en RAM",&font11,GREEN);
}
static void confirm(void){
 centered(48,"FIJAR REFERENCIA PROPIA",&font13,AMBER);
 if(view.count)clipped(12,71,width-24,view.aps[view.selected].ssid,&font19,WHITE);
 centered(101,"Guarda SSID, BSSID, canal y seguridad.",&font11,DIM);
 centered(122,"No guarda contrasenas.",&font11,DIM);
}
static void apply_geometry(void){if(applied_profile==requested_profile&&applied_invert==requested_invert)return;applied_profile=requested_profile;applied_invert=requested_invert;width=applied_profile?240:320;height=applied_profile?135:170;esp_lcd_panel_swap_xy(panel,true);esp_lcd_panel_mirror(panel,true,false);esp_lcd_panel_set_gap(panel,applied_profile?40:0,applied_profile?52:35);esp_lcd_panel_invert_color(panel,applied_invert);}static bool done(esp_lcd_panel_io_handle_t io,esp_lcd_panel_io_event_data_t*e,void*c){BaseType_t wake=pdFALSE;xSemaphoreGiveFromISR(flushed,&wake);return wake==pdTRUE;}
static void short_press(void){page=page==5?2:(page+1)%PAGE_COUNT;manual_until=millis()+60000;}static void double_press(void){if(page==0)sentinel_clear();else if(page==1||page==2)sentinel_next();manual_until=millis()+60000;}static void hold_press(void){manual_until=millis()+60000;if(page==0)sentinel_set_running(!view.running);else if(page==1)page=2;else if(page==2&&view.count)page=5;else if(page==3)sentinel_forget_baseline();else if(page==5){sentinel_baseline_selected();page=3;}}
static void button_task(void*unused){bool stable=true,raw=true;uint32_t changed=0,down=0,last_up=0;bool held=false,pending=false;while(1){uint32_t now=millis();bool level=gpio_get_level(0);if(level!=raw){raw=level;changed=now;}if(level!=stable&&now-changed>=30){stable=level;if(!stable){down=now;held=false;}else{button_presses++;if(held){button_holds++;pending=false;}else if(pending&&now-last_up<330){pending=false;double_press();}else{pending=true;last_up=now;}}}if(!stable&&!held&&now-down>=1600){held=true;hold_press();}if(pending&&now-last_up>=330){pending=false;short_press();}vTaskDelay(pdMS_TO_TICKS(10));}}
static void screen_task(void*unused){while(1){frame_ms=esp_timer_get_time()/1000;uint32_t now=millis();sentinel_snapshot(&view);if(manual(now)||page==5)last_rotation=now;else if(now-last_rotation>12000){page=(page+1)%PAGE_COUNT;last_rotation=now;}apply_geometry();rect(0,0,width,height,BG);header();if(page==0)spectrum();else if(page==1)networks();else if(page==2)fingerprint();else if(page==3)alerts();else if(page==4)evidence();else confirm();footer();while(xSemaphoreTake(flushed,0)==pdTRUE){}if(esp_lcd_panel_draw_bitmap(panel,0,0,width,height,pixels)==ESP_OK&&xSemaphoreTake(flushed,pdMS_TO_TICKS(1000))==pdTRUE)frames++;if(capture_requested){capture_requested=false;serial_blob((uint8_t*)pixels,width*height*2,width,height);xSemaphoreGive(capture_done);}vTaskDelay(pdMS_TO_TICKS(90));}}
void screen_start(int profile,bool invert){requested_profile=profile;requested_invert=invert;flushed=xSemaphoreCreateBinary();capture_done=xSemaphoreCreateBinary();pixels=heap_caps_malloc(170*320*2,MALLOC_CAP_DMA);assert(flushed&&capture_done&&pixels);spi_bus_config_t bus={.mosi_io_num=23,.miso_io_num=-1,.sclk_io_num=18,.quadwp_io_num=-1,.quadhd_io_num=-1,.max_transfer_sz=170*320*2};ESP_ERROR_CHECK(spi_bus_initialize(SPI2_HOST,&bus,SPI_DMA_CH_AUTO));esp_lcd_panel_io_handle_t io;esp_lcd_panel_io_spi_config_t cfg={.cs_gpio_num=15,.dc_gpio_num=2,.spi_mode=0,.pclk_hz=16000000,.trans_queue_depth=1,.lcd_cmd_bits=8,.lcd_param_bits=8,.on_color_trans_done=done};ESP_ERROR_CHECK(esp_lcd_new_panel_io_spi((esp_lcd_spi_bus_handle_t)SPI2_HOST,&cfg,&io));esp_lcd_panel_dev_config_t dev={.reset_gpio_num=4,.rgb_ele_order=LCD_RGB_ELEMENT_ORDER_RGB,.bits_per_pixel=16};ESP_ERROR_CHECK(esp_lcd_new_panel_st7789(io,&dev,&panel));ESP_ERROR_CHECK(esp_lcd_panel_reset(panel));ESP_ERROR_CHECK(esp_lcd_panel_init(panel));apply_geometry();ESP_ERROR_CHECK(esp_lcd_panel_disp_on_off(panel,true));gpio_config_t led={.pin_bit_mask=1ULL<<32,.mode=GPIO_MODE_OUTPUT};ESP_ERROR_CHECK(gpio_config(&led));gpio_set_level(32,1);gpio_config_t button={.pin_bit_mask=1ULL,.mode=GPIO_MODE_INPUT,.pull_up_en=GPIO_PULLUP_ENABLE};ESP_ERROR_CHECK(gpio_config(&button));assert(xTaskCreate(screen_task,"sentinel_ui",6144,NULL,3,NULL)==pdPASS);assert(xTaskCreate(button_task,"sentinel_button",3072,NULL,3,NULL)==pdPASS);}
void screen_page(int value){if(value>=0&&value<PAGE_COUNT){page=value;manual_until=millis()+60000;}}void screen_profile(int p,bool i){requested_profile=p;requested_invert=i;}bool screen_capture(void){while(xSemaphoreTake(capture_done,0)==pdTRUE){}capture_requested=true;return xSemaphoreTake(capture_done,pdMS_TO_TICKS(20000))==pdTRUE;}unsigned screen_frames(void){return frames;}unsigned screen_button_presses(void){return button_presses;}unsigned screen_button_holds(void){return button_holds;}int screen_button_level(void){return gpio_get_level(0);}int screen_current_page(void){return page;}int screen_current_profile(void){return applied_profile;}bool screen_current_invert(void){return applied_invert;}


