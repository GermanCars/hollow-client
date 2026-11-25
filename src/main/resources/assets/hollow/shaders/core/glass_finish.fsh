#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 o = 0.5 / vec2(textureSize(InSampler, 0)) * 1.5;
    vec3 c = texture(InSampler, texCoord).rgb * 2.0 + texture(InSampler, texCoord + o).rgb + texture(InSampler, texCoord - o).rgb;
    c /= 4.0;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, 1.28);
    float n = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
    fragColor = vec4(clamp(c * 1.02 + n * 0.012, 0.0, 1.0), 1.0);
}
