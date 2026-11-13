# Rat frame renderer

Renders the rat's animation clips to transparent PNG frames for the widget.
The bulging eyes from the Godot scene (RAT.tscn, EyeAnchor on Bip01_Head_010)
are rebuilt in `index.html` → `addEyes()`.

1. Put `rat_animated_embedded.gltf` in this folder and run `npm install three@0.170.0` here.
2. `python3 render_all.py . raw` (needs Python Playwright with Chromium)
3. Crop/resize to 360px wide WebP into `android/app/src/main/res/drawable-nodpi/rat_*.webp`
   (crop box 320,277 → 844,580 on the 1200×780 renders).
4. `python3 tools/make_rat_layouts.py` from the repo root.
