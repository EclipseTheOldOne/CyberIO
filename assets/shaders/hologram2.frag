#define Density 1.3
#define OpacityScanline .3
#define Holo vec3(0.2588, 0.6471, 0.9608)
uniform sampler2D u_texture;
uniform float u_time;
//uniform vec2 u_resolution;
uniform float u_alpha;//1f as default
uniform float u_opacityNoise;//0.2f as default
uniform float u_flickering;//0.03f as default
uniform bool u_blendHoloColorB;//true as default
uniform bool u_blendFormerColorB;//true as default
varying vec2 v_texCoords;
varying lowp vec4 v_color;

float random(vec2 st) {
    return fract(sin(dot(st.xy,
    vec2(12.9898, 78.233)))*
    43758.5453123);
}

float blend(float x, float y) {
    return (x < 0.5) ? (2.0 * x * y) : (1.0 - 2.0 * (1.0 - x) * (1.0 - y));
}

vec3 blend(vec3 x, vec3 y, float opacity) {
    vec3 z = vec3(blend(x.r, y.r), blend(x.g, y.g), blend(x.b, y.b));
    return z * opacity + x * (1.0 - opacity);
}

void main(){
    vec2 uv = v_texCoords.xy;
    vec4 original = texture2D(u_texture, uv);
    if (original.a < 0.01){
        return;
    }
    vec3 col = original.rgb;
    if (u_blendFormerColorB){
        col = blend(col, v_color.rgb, 0.60);
    }
    if (u_blendHoloColorB){
        col = blend(col, Holo, 0.8);
    }
    float count = Density;
    vec2 sl = vec2(sin(uv.y * count), cos(uv.y * count));
    vec3 scanlines = vec3(sl.x, sl.y, sl.x);

    col += col * scanlines * OpacityScanline;
    col += col * vec3(random(uv * u_time)) * u_opacityNoise;
    col += col * sin(110.0 * u_time) * u_flickering;

    gl_FragColor = vec4(col-0.25, u_alpha);
}