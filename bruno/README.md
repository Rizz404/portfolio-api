# Koleksi Bruno

Koleksi utama berisi contoh request dan fixture untuk pengujian API.

Untuk pengujian dengan kredensial atau data asli, buka folder `bruno/private/`
sebagai koleksi terpisah di Bruno. Folder ini merupakan salinan lokal request dan
fixture, diabaikan seluruhnya oleh Git, dan dikecualikan dari pemindaian koleksi
utama. Simpan kredensial, payload, file upload, dan hasil pengujian pribadi di
dalam folder tersebut.

Environment `local` pada koleksi pribadi memakai
`http://localhost:8080/api/v1` dengan `TOKEN` kosong. Sesuaikan environment dan
request di koleksi pribadi sesuai kebutuhan. Koleksi ini mencakup request yang
membuat, mengubah, dan menghapus data; jalankan pada data pengujian yang memang
boleh diubah. Aturan Git hanya mencegah file ikut commit, bukan membatasi akses
request ke API.

Folder `private/` hanya tersedia di mesin tempat folder tersebut dibuat dan tidak
ikut saat clone. Untuk membuatnya kembali dari root repository:

```sh
mkdir -p bruno/private/environments
cp bruno/opencollection.yml bruno/private/opencollection.yml
cp bruno/environments/local.yml bruno/private/environments/local.yml
cp -R bruno/auth bruno/blog-attachments bruno/blogs bruno/experiences \
  bruno/fixtures bruno/projects bruno/skills bruno/users bruno/uses bruno/private/
```

Salinan bersifat mandiri; perubahan request pada koleksi utama tidak otomatis
diterapkan ke koleksi pribadi.
