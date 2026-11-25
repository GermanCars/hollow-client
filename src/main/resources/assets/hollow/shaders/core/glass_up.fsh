#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 o = 0.5 / vec2(textureSize(InSampler, 0)) * 1.5;
    vec3 c = texture(InSampler, texCoord + vec2(-o.x * 2.0, 0.0)).rgb + texture(InSampler, texCoord + vec2(o.x * 2.0, 0.0)).rgb + texture(InSampler, texCoord + vec2(0.0, o.y * 2.0)).rgb + texture(InSampler, texCoord + vec2(0.0, -o.y * 2.0)).rgb;
    c += 2.0 * (texture(InSampler, texCoord + vec2(-o.x, o.y)).rgb + texture(InSampler, texCoord + o).rgb + texture(InSampler, texCoord + vec2(o.x, -o.y)).rgb + texture(InSampler, texCoord - o).rgb);
    fragColor = vec4(c / 12.0, 1.0);
}
