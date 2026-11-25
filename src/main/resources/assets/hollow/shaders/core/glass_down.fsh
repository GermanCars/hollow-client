#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 o = 0.5 / vec2(textureSize(InSampler, 0)) * 1.6;
    vec3 c = texture(InSampler, texCoord).rgb * 4.0;
    c += texture(InSampler, texCoord - o * 2.0).rgb + texture(InSampler, texCoord + o * 2.0).rgb;
    c += texture(InSampler, texCoord + vec2(o.x, -o.y) * 2.0).rgb + texture(InSampler, texCoord - vec2(o.x, -o.y) * 2.0).rgb;
    fragColor = vec4(c / 8.0, 1.0);
}
