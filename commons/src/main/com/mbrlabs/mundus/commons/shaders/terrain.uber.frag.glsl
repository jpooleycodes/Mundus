/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifdef GL_ES
#define LOW lowp
#define MED mediump
#define HIGH highp
precision highp float;
#else
#define MED
#define LOW
#define HIGH
#endif

#define PI 3.1415926535897932384626433832795

const MED vec4 COLOR_TURQUOISE = vec4(0,0.714,0.586, 1.0);
const MED vec4 COLOR_WHITE = vec4(1,1,1, 1.0);
const MED vec4 COLOR_DARK = vec4(0.05,0.05,0.05, 1.0);
const MED vec4 COLOR_BRIGHT = vec4(0.8,0.8,0.8, 1.0);
const MED vec4 COLOR_BRUSH = vec4(0.4,0.4,0.4, 0.4);

// splat textures
uniform sampler2D u_baseTexture;
uniform sampler2D u_texture_base_normal;

#if defined(heightLayer) || defined(slopeLayer)
// Needed by both layers
struct Layer
{
    vec2 minMaxHeight;
    float slopeStrength;
    sampler2D texture;
    sampler2D normalTexture;
};

varying vec3 v_normal; // Vertex Normal
varying vec3 v_localPos; // Vertex Position
#endif

#ifdef heightLayer
uniform int u_activeHeightLayers;
uniform Layer u_heightLayers[4];
#endif

#ifdef slopeLayer
uniform int u_activeSlopeLayers;
uniform Layer u_slopeLayers[4];
#endif

#if defined(heightLayer) || defined(slopeLayer)
#endif

#ifdef splatFlag
    varying vec2 v_splatPosition;
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

#endif // splatFlag

#ifdef fogFlag
uniform vec3 u_fogEquation;
uniform MED vec4 u_fogColor;
#endif

// mouse picking
#ifdef PICKER
uniform vec3 u_pickerPos;
uniform float u_pickerRadius;
uniform int u_pickerActive;
varying vec3 v_pos;
#endif

// light
varying mat3 v_TBN;

varying MED vec2 v_texCoord0;
varying float v_clipDistance;

float normalizeRange(float value, float minValue, float maxValue) {
    float weight = max(minValue, value);
    weight = min(maxValue, weight);
    weight -= minValue;
    weight /= maxValue - minValue; // Normalizes to 0.0-1.0 range
    return weight;
}

void main(void) {
    if ( v_clipDistance < 0.0 )
        discard;

    // Terrains always have a base texture, so we sample it first
    gl_FragColor = texture2D(u_baseTexture, v_texCoord0);

    vec3 normal = texture2D(u_texture_base_normal, v_texCoord0).rgb;

    #ifdef heightLayer
    for (int i = 0 ; i < maxLayers; i++) {
        if (i >= u_activeHeightLayers){break;}

        float blend = normalizeRange(v_localPos.y, u_heightLayers[i].minMaxHeight.x /*+ noises*/, u_heightLayers[i].minMaxHeight.y /*+ noises*/);
        gl_FragColor = mix(gl_FragColor, texture2D(u_heightLayers[i].texture, v_texCoord0), blend);

        // Blend normals always, to remove base normal from appearing over the layer
        normal = mix(normal, texture2D(u_heightLayers[i].normalTexture, v_texCoord0).rgb, blend);
    }
    #endif

    #ifdef slopeLayer
    // Perform Slope Layers after Height layers so that they end up on top
    for (int i = 0 ; i < maxLayers; i++) {
        if (i >= u_activeSlopeLayers) continue;

        // Slope blending
        float blendHeight = normalizeRange(v_localPos.y, u_slopeLayers[i].minMaxHeight.x, u_slopeLayers[i].minMaxHeight.y);

        // Take the surface normal, factor in the blend height and strength.
        float slopeBlend =  (1.0 - abs(v_normal.y)) * blendHeight * u_slopeLayers[i].slopeStrength;
        slopeBlend = clamp(slopeBlend, 0.0, 1.0);

        gl_FragColor = mix(gl_FragColor, texture2D(u_slopeLayers[i].texture, v_texCoord0), slopeBlend);
        normal = mix(normal, texture2D(u_slopeLayers[i].normalTexture, v_texCoord0).rgb, slopeBlend);
    }
    #endif

    // Mix splat textures
    #ifdef splatFlag
    vec4 splat = texture2D(u_texture_splat, v_splatPosition);
        #ifdef splatRFlag
            gl_FragColor = mix(gl_FragColor, texture2D(u_texture_r, v_texCoord0), splat.r);
        #endif
        #ifdef splatGFlag
            gl_FragColor = mix(gl_FragColor, texture2D(u_texture_g, v_texCoord0), splat.g);
        #endif
        #ifdef splatBFlag
            gl_FragColor = mix(gl_FragColor, texture2D(u_texture_b, v_texCoord0), splat.b);
        #endif
        #ifdef splatAFlag
            gl_FragColor = mix(gl_FragColor, texture2D(u_texture_a, v_texCoord0), splat.a);
        #endif

        #ifdef normalTextureFlag
            // Splat normals
            #ifdef splatRNormalFlag
                normal = mix(normal, texture2D(u_texture_r_normal, v_texCoord0).rgb, splat.r);
            #endif
            #ifdef splatGNormalFlag
                normal = mix(normal, texture2D(u_texture_g_normal, v_texCoord0).rgb, splat.g);
            #endif
            #ifdef splatBNormalFlag
                normal = mix(normal, texture2D(u_texture_b_normal, v_texCoord0).rgb, splat.b);
            #endif
            #ifdef splatANormalFlag
                normal = mix(normal, texture2D(u_texture_a_normal, v_texCoord0).rgb, splat.a);
            #endif

        #endif

    #endif

    #ifdef normalTextureFlag
        normal = normalize(v_TBN * ((2.0 * normal - 1.0)));
    #else
        normal = normalize(v_TBN[2].xyz);
    #endif

    // =================================================================
    //                          Lighting
    // =================================================================
    vec4 totalLight = CalcDirectionalLight(normal);

    for (int i = 0 ; i < numPointLights ; i++) {
        if (i >= u_activeNumPointLights){break;}
        totalLight += CalcPointLight(u_pointLights[i], normal);
    }

    for (int i = 0; i < numSpotLights; i++) {
        if (i >= u_activeNumSpotLights){break;}
        totalLight += CalcSpotLight(u_spotLights[i], normal);
    }

    gl_FragColor *= totalLight;
    // =================================================================
    //                          /Lighting
    // =================================================================

    #ifdef fogFlag
    // fog
    vec3 surfaceToCamera = u_cameraPosition.xyz - v_worldPos;
    float eyeDistance = length(surfaceToCamera);

    float fog = (eyeDistance - u_fogEquation.x) / (u_fogEquation.y - u_fogEquation.x);
    fog = clamp(fog, 0.0, 1.0);
    fog = pow(fog, u_fogEquation.z);

    gl_FragColor.rgb = mix(gl_FragColor.rgb, u_fogColor.rgb, fog * u_fogColor.a);
    #endif

    #ifdef PICKER
    if(u_pickerActive == 1) {
        float dist = distance(u_pickerPos, v_pos);
        if(dist <= u_pickerRadius) {
            float gradient = (u_pickerRadius - dist + 0.01) / u_pickerRadius;
            gradient = 1.0 - clamp(cos(gradient * PI), 0.0, 1.0);
            gl_FragColor += COLOR_BRUSH * gradient;
        }
    }
    #endif

}
