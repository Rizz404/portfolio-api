# Advanced filtering

Semua endpoint daftar memakai prefix `/api/v1`. Parameter bersifat opsional;
request lama tetap bisa digunakan. Antarfilter digabung dengan **AND**, sedangkan
beberapa nilai dalam satu filter enum/JSON digabung dengan **OR**.

Enum menerima huruf besar/kecil. Pisahkan beberapa nilai dengan koma atau ulangi
parameter, misalnya `status=active,development` atau
`status=active&status=development`. Nilai enum yang tidak dikenal menghasilkan
HTTP 400 beserta daftar nilai yang valid. Parameter kosong tidak menerapkan filter.

## Filter umum

| Parameter | Perilaku |
| --- | --- |
| `ids` | Salah satu ID, misalnya `ids=10,20` atau `ids=10&ids=20` |
| `createdFrom`, `createdTo` | Batas inklusif `createdAt` |
| `updatedFrom`, `updatedTo` | Batas inklusif `updatedAt` |
| `page`, `size` | Page mulai 1; size 1–100, default 10 |
| `sortBy`, `sortDir` | Satu field dan satu arah `asc`/`desc`; default `createdAt desc` |
| `cursor` | Ambil ID lebih kecil dari cursor, selalu dengan urutan `id desc` |

Timestamp memakai ISO 8601 dengan zona waktu, misalnya
`createdFrom=2026-01-01T00:00:00Z`. Untuk offset `+07:00`, encode tanda `+` menjadi
`%2B` pada URL. Rentang terbalik menghasilkan HTTP 400. Filter diterapkan sebelum
pagination dan penghitungan total.

Sorting dilakukan di database sebelum pagination. Teks diurutkan tanpa membedakan
huruf besar/kecil, dengan collation database; ini bukan urutan kamus bahasa atau
pengurutan berdasarkan relevansi. NULL selalu berada terakhir untuk kedua arah.
ID menjadi pemecah seri (`id desc`) jika belum diminta sebagai field sorting.

Hanya **satu `sortBy` dan satu `sortDir`** yang aktif, misalnya
`sortBy=name&sortDir=asc`. Mengirim beberapa nilai dengan koma atau mengulang
parameter sorting menghasilkan HTTP 400, termasuk jika nilainya sama.
Nilai kosong, field tidak didukung, dan arah tidak valid juga menghasilkan 400.
Filter tetap bisa memakai beberapa nilai dan digabung dengan filter lain.

Cursor hanya mendukung `id desc`. Sorting tidak dikirim otomatis memilih `id desc`
saat memakai cursor. Custom sorting bersama cursor menghasilkan HTTP 400;
gunakan pagination `page` untuk sorting nama/judul atau field lain. Gunakan cursor
ID terbesar, misalnya `9223372036854775807`, untuk memulai cursor pagination, lalu
kirim `cursor.nextCursor` pada request berikutnya dengan filter yang sama.

## Bahasa pencarian dan sorting

Field translation hanya dicari dan diurutkan pada **bahasa request saat ini**
dari `Accept-Language` (`id` atau `en`), tanpa fallback ke bahasa lain.
Header tidak dikirim atau bahasa tidak didukung memakai bahasa default `en`.

Jika salah satu field sorting berada di tabel translation, hasil hanya mencakup
resource yang memiliki baris translation bahasa request. Batasan ini juga dipakai
untuk menghitung total pagination. Jika baris tersebut ada tetapi field-nya NULL,
resource tetap masuk dengan NULL terakhir; nilai Inggris tidak dipakai sebagai
pengganti. Join translation dibatasi per bahasa dan digunakan bersama oleh search
dan sorting agar tidak menggandakan resource.

Contoh: nama project Inggris `Zulu`, nama Indonesia `Zebra`. Dengan
`Accept-Language: id`, `search=Zulu` tidak cocok dan `sortBy=name` memakai `Zebra`.
Project yang hanya punya translation Inggris tidak masuk dalam sorting nama pada
bahasa Indonesia.

Pencarian field utama tetap dapat cocok tanpa translation bahasa request, misalnya
`search=Acme` pada experience melalui `companyName`. Sorting field utama saja juga
tidak mensyaratkan translation bahasa request. Dalam kedua situasi ini, mapper
response tetap memakai perilaku fallback yang sudah ada; pembatasan bahasa di atas
berlaku untuk pencocokan dan pengurutan **field translation**.

## Field sorting

Semua endpoint mendukung `id`, `createdAt`, dan `updatedAt`. Field tambahan:

| Endpoint | Field utama / relasi | Field translation |
| --- | --- | --- |
| `/projects` | `slug`, `status` | `name`, `description` |
| `/blogs` | `slug`, `isPublished`, `viewsCount`, `likesCount`, `dislikesCount` | `title`, `content` |
| `/experiences` | `companyName`, `startDate`, `endDate`, `isCurrent` | `position`, `description` |
| `/skills` | `name`, `category`, `logoUrl` | `description` |
| `/uses` | `itemName`, `category`, `logoUrl` | `reasons` |
| `/users` | `nickname`, `fullName`, `email`, `role`, `provider`, `gender`, `dateOfBirth`, `placeOfBirth`, `address`, `phoneNumber` | `bio` |
| `/blog-attachments` | `fileName`, `fileUrl`, `fileType`, `blogId` | — |

Nama field bersifat case-sensitive. Enum diurutkan menurut nilai teks database,
tanpa prioritas bisnis khusus. Array/object seperti `jobdesks`, `projectTypes`,
`projectLinks`, dan `techStack` tidak mendukung sorting biasa dan menghasilkan 400.

## Filter tiap resource

| Endpoint | Parameter tambahan |
| --- | --- |
| `/projects` | `search`, `status`, `slug`, `projectTypes`, `linkTypes`, `techStack` |
| `/skills` | `search`, `category` |
| `/uses` | `search`, `category` |
| `/blogs` | `search`, `slug`, `isPublished`, `minViews`, `maxViews` |
| `/blog-attachments` | `search` (nama file), `blogId`, `fileType` |
| `/experiences` | `search`, `isCurrent`, `startDate`, `endDate`, `companyName`, `position` |
| `/users` | `search`, `role`, `provider`, `gender`, `email`, `nickname`, `dateOfBirthFrom`, `dateOfBirthTo` |

`slug`, `email`, dan `nickname` memakai kecocokan persis; email/nickname mengabaikan
huruf besar/kecil. `search`, `companyName`, dan `position` mencari substring.
Pencarian pada field terjemahan dibatasi pada `Accept-Language` seperti dijelaskan
di bagian bahasa pencarian dan sorting.

Project `projectTypes` memeriksa anggota array JSONB, `linkTypes` memeriksa key
`projectLinks`, dan `techStack` memeriksa nama/key teknologi secara persis dan
case-sensitive. Semuanya menerima beberapa nilai. Memakai filter JSON pada field
NULL tidak akan menghasilkan kecocokan. Pemeriksaan anggota/key menggunakan
fungsi PostgreSQL [`jsonb_exists`](https://doxygen.postgresql.org/jsonb__op_8c_source.html).

`isPublished` dan `isCurrent` mendukung `true` maupun `false`. Jika tidak dikirim,
tidak ada filter boolean. Experience `startDate` berarti tanggal mulai >= nilai
parameter dan `endDate` berarti tanggal selesai <= nilai parameter (experience
yang tanggal selesainya NULL tidak cocok). Tanggal memakai format `YYYY-MM-DD`.
`minViews`/`maxViews` dan rentang tanggal lahir memakai batas inklusif.

## Nilai enum

| Parameter | Nilai |
| --- | --- |
| Project `status` | `active`, `inactive`, `development`, `maintenance`, `archived` |
| Project `projectTypes` | `frontend`, `backend`, `fullstack`, `mobile`, `desktop`, `api`, `library`, `other` |
| Project `linkTypes` | `github`, `gitlab`, `bitbucket`, `source_code`, `demo`, `website`, `figma`, `documentation`, `api_docs`, `video`, `playstore`, `appstore`, `npm`, `dockerhub`, `staging`, `other` |
| Skill `category` | `programming_language`, `framework`, `database`, `tool`, `other` |
| Use `category` | `software`, `hardware` |
| Attachment `fileType` | `image`, `document`, `video`, `audio`, `archive`, `other` |
| User `role` | `USER`, `ADMIN` |
| User `provider` | `LOCAL`, `GITHUB` |
| User `gender` | `MALE`, `FEMALE`, `OTHER`, `PREFER_NOT_TO_SAY` |

## Contoh request

```http
GET /api/v1/projects?status=active&projectTypes=backend&linkTypes=github
GET /api/v1/projects?status=active,development&projectTypes=backend,api&techStack=Spring
GET /api/v1/skills?category=framework,database&search=sql
GET /api/v1/uses?category=software&ids=10,30
GET /api/v1/blogs?isPublished=false&minViews=5&maxViews=100
GET /api/v1/blog-attachments?blogId=10&fileType=image,document
GET /api/v1/experiences?isCurrent=false&companyName=acme&startDate=2021-01-01
GET /api/v1/users?role=ADMIN,USER&provider=LOCAL&gender=FEMALE
GET /api/v1/projects?status=active,development&sortBy=name&sortDir=asc
Accept-Language: id

GET /api/v1/blogs?isPublished=true&sortBy=title&sortDir=asc
Accept-Language: en

GET /api/v1/blog-attachments?sortBy=blogId&sortDir=asc
```

Cache daftar mencakup locale, semua filter, sorting, dan pagination. Prefix cache
daftar memakai `list:v4:` agar hasil sorting lama tidak dipakai kembali.

## Pengujian PostgreSQL

Integration test menjalankan migrasi Flyway pada database pengujian khusus,
memasukkan fixture dalam transaksi, lalu rollback setiap test. Cache pengujian
memakai cache lokal agar tidak membutuhkan Redis. Gunakan database kosong terpisah.

```sh
FILTER_TEST_DATABASE_URL='jdbc:postgresql://127.0.0.1:5432/portfolio_filter_test?user=postgres&password=postgres' \
  ./mvnw -Dtest=AdvancedFilteringIntegrationTests test
```

Test ini dilewati jika `FILTER_TEST_DATABASE_URL` tidak disetel. Jalankan dengan
JDK 25 sesuai `pom.xml`.
