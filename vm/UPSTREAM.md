# VM upstream

- Source: https://github.com/magicus/phoneME (git conversion of the archived java.net phoneME SVN)
- Commit: 5a13f65cff07b2211b7b867c51ada4bb9c7d4f32
- Imported: `cldc/` → `vm/phoneme-cldc/`, `legal/` → `vm/phoneme-legal/`
- License: GPLv2 only (see `vm/phoneme-legal/license.txt`)
- VM: CLDC-HI with the portable C interpreter loop (`src/vm/cpu/c`, see `README.cloop`)

Our changes to the imported tree are separate git commits on top of the pristine import
(`git log -- vm/phoneme-cldc`), each marked `amiga-j2me:` in the code where practical.
