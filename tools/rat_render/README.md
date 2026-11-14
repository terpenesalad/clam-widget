# Rat frame renderer

Renders the rat's animation clips to transparent PNG frames for the widget.
The bulging eyes from the Godot scene (RAT.tscn, EyeAnchor on Bip01_Head_010)
are rebuilt in `index.html` → `addEyes()`.

1. Put `rat_animated_embedded.gltf` in this folder and run `npm install three@0.170.0` here.
2. `python3 render_all.py . raw` (needs Python Playwright with Chromium)
3. Crop/resize to 360px wide WebP into `android/app/src/main/res/drawable-nodpi/rat_*.webp`
   (crop box 313,277 → 865,582 on the 1200×780 renders). Idle mode clips render at 20fps
   with `render_one.py . raw20 FBXExportClip_0 idle 20` (and the same for each extra).
4. `python3 tools/make_rat_layouts.py` from the repo root.

## Paw colour
`recolor.py src.gltf out.gltf 0.8` recolours the paws (below mesh height 0.8, found
via the foot/hand bone weights) to the tail/ear colour. `to_glb.py out.gltf rat.glb`
packs it as a GLB. The widget frames are rendered from the recoloured model.

## Fur colour (matte, dark brown)
The fur material is rendered matte (glTF defaults metalness to 1, which caused a white sheen).
`darken.py rat.gltf dark.gltf 0.27 1.6 1.8` darkens/warms only the brown fur (tail, pink and
white fur untouched); run `recolor.py` on its output for the paws, then render from that.
The full-screen QR screen uses its own 60fps idle frames (`rat_qr_*`, 540px wide).
