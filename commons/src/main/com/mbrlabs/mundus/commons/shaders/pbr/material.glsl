#ifdef normalFlag
#ifdef tangentFlag
varying mat3 v_TBN;
#else
varying vec3 v_normal;
#endif

#endif //normalFlag

#if defined(colorFlag)
varying vec4 v_color;
#endif

#ifdef blendedFlag
uniform float u_opacity;
#ifdef alphaTestFlag
uniform float u_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#ifdef textureFlag
varying MED vec2 v_texCoord0;
#ifdef triplanarFlag
uniform mat3 u_texCoord0Transform;
#endif // triplanarFlag
#endif // textureFlag

#ifdef textureCoord1Flag
varying MED vec2 v_texCoord1;
#endif // textureCoord1Flag

// texCoord unit mapping

#ifndef v_diffuseUV
#define v_diffuseUV v_texCoord0
#endif

#ifndef v_emissiveUV
#define v_emissiveUV v_texCoord0
#endif

#ifndef v_normalUV
#define v_normalUV v_texCoord0
#endif

#ifndef v_occlusionUV
#define v_occlusionUV v_texCoord0
#endif

#ifndef v_metallicRoughnessUV
#define v_metallicRoughnessUV v_texCoord0
#endif

#ifndef v_transmissionUV
#define v_transmissionUV v_texCoord0
#endif

#ifndef v_thicknessUV
#define v_thicknessUV v_texCoord0
#endif

#ifndef v_specularFactorUV
#define v_specularFactorUV v_texCoord0
#endif

#ifndef v_specularColorUV
#define v_specularColorUV v_texCoord0
#endif

#ifndef v_iridescenceUV
#define v_iridescenceUV v_texCoord0
#endif

#ifndef v_iridescenceThicknessUV
#define v_iridescenceThicknessUV v_texCoord0
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef baseColorFactorFlag
uniform vec4 u_BaseColorFactor;
#endif

#ifdef triplanarFlag
// getColor == triplanar method
#define getColor triplanar
const float scaleAdjust = 0.01;
vec4 triplanar(sampler2D diffuseTexture, vec3 triblend)
{
	vec2 uvX = v_position.zy * scaleAdjust;
	vec2 uvY = v_position.xz * scaleAdjust;
	vec2 uvZ = v_position.xy * scaleAdjust;

	// Apply tex coord transforms
	uvX = (u_texCoord0Transform * vec3(uvX, 1.0)).xy;
	uvY = (u_texCoord0Transform * vec3(uvY, 1.0)).xy;
	uvZ = (u_texCoord0Transform * vec3(uvZ, 1.0)).xy;

	// project+fetch
	vec4 x = texture2D(diffuseTexture, uvX);
	vec4 y = texture2D(diffuseTexture, uvY);
	vec4 z = texture2D(diffuseTexture, uvZ);
	vec4 col = x * triblend.x + y * triblend.y + z * triblend.z;

	return col;
}
#else
// getColor == texture2D method
#define getColor texture2D
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef splatFlag
varying vec2 v_splatPosition;
vec4 splat;
uniform sampler2D u_texture_splat;
    #ifdef splatRFlag
    uniform sampler2D u_texture_r;
    #endif
    #ifdef splatGFlag
    uniform sampler2D u_texture_g;
    #endif
    #ifdef splatBFlag
    uniform sampler2D u_texture_b;
    #endif
    #ifdef splatAFlag
    uniform sampler2D u_texture_a;
    #endif

    #ifdef splatRNormalFlag
    uniform sampler2D u_texture_r_normal;
    #endif
    #ifdef splatGNormalFlag
    uniform sampler2D u_texture_g_normal;
    #endif
    #ifdef splatBNormalFlag
    uniform sampler2D u_texture_b_normal;
    #endif
    #ifdef splatANormalFlag
    uniform sampler2D u_texture_a_normal;
    #endif
#endif

// mouse picking
#ifdef PICKER
#define PI 3.1415926535897932384626433832795
const MED vec4 COLOR_BRUSH = vec4(0.4,0.4,0.4, 0.4);
uniform vec3 u_pickerPos;
uniform float u_pickerRadius;
uniform int u_pickerActive;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
uniform float u_NormalScale;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#endif

#ifdef occlusionTextureFlag
uniform sampler2D u_OcclusionSampler;
uniform float u_OcclusionStrength;
#endif

#ifdef metallicRoughnessTextureFlag
uniform sampler2D u_MetallicRoughnessSampler;
#endif

#ifdef transmissionTextureFlag
uniform sampler2D u_transmissionSampler;
#endif

#ifdef transmissionFlag
uniform float u_transmissionFactor;
#endif

#ifdef volumeFlag
uniform float u_thicknessFactor;
uniform float u_attenuationDistance;
uniform vec3 u_attenuationColor;
#endif

#ifdef thicknessTextureFlag
uniform sampler2D u_thicknessSampler;
#endif

#ifdef iorFlag
uniform float u_ior;
#else
#define u_ior 1.5
#endif

#ifdef specularFactorFlag
uniform float u_specularFactor;
#else
#define u_specularFactor 1.0
#endif

#ifdef specularColorFlag
uniform vec3 u_specularColorFactor;
#endif

#ifdef specularFactorTextureFlag
uniform sampler2D u_specularFactorSampler;
#endif

#ifdef specularColorTextureFlag
uniform sampler2D u_specularColorSampler;
#endif

#ifdef iridescenceFlag
uniform float u_iridescenceFactor;
uniform float u_iridescenceIOR;
uniform float u_iridescenceThicknessMin;
uniform float u_iridescenceThicknessMax;
#endif

#ifdef iridescenceTextureFlag
uniform sampler2D u_iridescenceSampler;
#endif

#ifdef iridescenceThicknessTextureFlag
uniform sampler2D u_iridescenceThicknessSampler;
#endif

uniform vec2 u_MetallicRoughnessValues;

// Encapsulate the various inputs used by the various functions in the shading equation
// We store values in structs to simplify the integration of alternative implementations
// PBRSurfaceInfo contains light independant information (surface/material only)
// PBRLightInfo contains light information (incident rays)
struct PBRSurfaceInfo
{
	vec3 n;						  // Normal vector at surface point
	vec3 v;						  // Vector from surface point to camera
	float NdotV;                  // cos angle between normal and view direction
	float perceptualRoughness;    // roughness value, as authored by the model creator (input to shader)
	float metalness;              // metallic value at the surface
	vec3 reflectance0;            // full reflectance color (normal incidence angle)
	vec3 reflectance90;           // reflectance color at grazing angle
	float alphaRoughness;         // roughness mapped to a more linear change in the roughness (proposed by [2])
	vec3 diffuseColor;            // color contribution from diffuse lighting
	vec3 specularColor;           // color contribution from specular lighting

	float thickness;           	  // volume thickness at surface point (used for refraction)

	float specularWeight;		  // Amount of specular for the material (default is 1.0)

#ifdef iridescenceFlag
	float iridescenceFactor;
	float iridescenceIOR;
	float iridescenceThickness;
	vec3 iridescenceFresnel;
	vec3 iridescenceF0;
#endif
};

vec4 getBaseColor()
{
    // The albedo may be defined from a base texture or a flat color
#ifdef baseColorFactorFlag
	vec4 baseColorFactor = u_BaseColorFactor;
#else
	vec4 baseColorFactor = vec4(1.0, 1.0, 1.0, 1.0);
#endif

#ifdef triplanarFlag
    #define getColor triplanar
    vec3 colorUv = clamp(pow(abs(v_TBN[2]), vec3(4.0)), vec3(0.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0));
    colorUv /= dot(colorUv, vec3(1.0,1.0,1.0));
#else
    #ifdef diffuseTextureFlag
    vec2 colorUv = v_diffuseUV;
    #else
    vec2 colorUv = vec2(0.0);
    #endif
    #define getColor texture2D
#endif

#ifdef diffuseTextureFlag
    vec4 baseColor = getColor(u_diffuseTexture, colorUv);

    #ifdef splatFlag
        splat = texture2D(u_texture_splat, v_splatPosition);
        #ifdef splatRFlag
        vec4 colorR = getColor(u_texture_r, colorUv);
        baseColor = mix(baseColor, mix(baseColor, colorR, splat.r), colorR.a);
        #endif
        #ifdef splatGFlag
        vec4 colorG = getColor(u_texture_g, colorUv);
        baseColor = mix(baseColor, mix(baseColor, colorG, splat.g), colorG.a);
        #endif
        #ifdef splatBFlag
        vec4 colorB = getColor(u_texture_b, colorUv);
        baseColor = mix(baseColor, mix(baseColor, colorB, splat.b), colorB.a);
        #endif
        #ifdef splatAFlag
        vec4 colorA = getColor(u_texture_a, colorUv);
        baseColor = mix(baseColor, mix(baseColor, colorA, splat.a), colorA.a);
        #endif
    #endif // splatFlag

    baseColor = SRGBtoLINEAR(baseColor) * baseColorFactor;
#else
    vec4 baseColor = baseColorFactor;
#endif

#ifdef colorFlag
    baseColor *= v_color;
#endif
    return baseColor;
}

#ifndef unlitFlag
// Find the normal for this fragment, pulling either from a predefined normal map
// or from the interpolated mesh normal and tangent attributes.
vec3 getNormal()
{
#ifdef tangentFlag
#ifdef normalTextureFlag

    #ifdef triplanarFlag
        vec3 colorUv = clamp(pow(abs(v_TBN[2]), vec3(4.0)), vec3(0.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0));
        colorUv /= dot(colorUv, vec3(1.0,1.0,1.0));
    #else
        vec2 colorUv = v_diffuseUV;
    #endif

    vec3 n = getColor(u_normalTexture, colorUv).rgb;

    #ifdef splatFlag
        vec3 splatNormal = vec3(0.0);
    #ifdef splatRNormalFlag
        splatNormal += getColor(u_texture_r_normal, colorUv).rgb * splat.r;
    #endif
    #ifdef splatGNormalFlag
        splatNormal += getColor(u_texture_g_normal, colorUv).rgb * splat.g;
    #endif
    #ifdef splatBNormalFlag
        splatNormal += getColor(u_texture_b_normal, colorUv).rgb * splat.b;
    #endif
    #ifdef splatANormalFlag
        splatNormal += getColor(u_texture_a_normal, colorUv).rgb * splat.a;
    #endif
    float normalBlendFactor = (1.0 - splat.r - splat.g - splat.b - splat.a);
    n = (n * normalBlendFactor) + splatNormal;
    #endif // splatFlag

    n = normalize(v_TBN * ((2.0 * n - 1.0) * vec3(u_NormalScale, u_NormalScale, 1.0)));
#else
    vec3 n = normalize(v_TBN[2].xyz);
#endif
#else
    vec3 n = normalize(v_normal);
#endif

    return n;
}
#endif

float getTransmissionFactor()
{
#ifdef transmissionFlag
    float transmissionFactor = u_transmissionFactor;
#ifdef transmissionTextureFlag
    transmissionFactor *= texture2D(u_transmissionSampler, v_transmissionUV).r;
#endif
    return transmissionFactor;
#else
    return 0.0;
#endif
}

float getThickness()
{
#ifdef volumeFlag
	float thickness = u_thicknessFactor;
#ifdef thicknessTextureFlag
	thickness *= texture2D(u_thicknessSampler, v_thicknessUV).g;
#endif
	return thickness;
#else
	return 0.0;
#endif
}

#ifdef iridescenceFlag
PBRSurfaceInfo getIridescenceInfo(PBRSurfaceInfo info){
	info.iridescenceFactor = u_iridescenceFactor;
	info.iridescenceIOR = u_iridescenceIOR;
	info.iridescenceThickness = u_iridescenceThicknessMax;

#ifdef iridescenceTextureFlag
	info.iridescenceFactor *= texture2D(u_iridescenceSampler, v_iridescenceUV).r;
#endif

#ifdef iridescenceThicknessTextureFlag
	float thicknessFactor = texture2D(u_iridescenceThicknessSampler, v_iridescenceThicknessUV).g;
	info.iridescenceThickness = mix(u_iridescenceThicknessMin, u_iridescenceThicknessMax, thicknessFactor);
#endif

    info.iridescenceFresnel = info.specularColor;
    info.iridescenceF0 = info.specularColor;

    if (info.iridescenceThickness == 0.0) {
    	info.iridescenceFactor = 0.0;
    }

    if (info.iridescenceFactor > 0.0) {
    	info.iridescenceFresnel = evalIridescence(1.0, info.iridescenceIOR, info.NdotV, info.iridescenceThickness, info.specularColor);
    	info.iridescenceF0 = Schlick_to_F0(info.iridescenceFresnel, info.NdotV);
    }

	return info;
}
#endif
