# Supabase Storage — Açaívis

O backend usa o bucket `product-images` para armazenar imagens de produtos.

## Variáveis de ambiente

```env
SUPABASE_URL=https://SEU-PROJETO.supabase.co
SUPABASE_SECRET_KEY=sb_secret_...
```

A `SUPABASE_SECRET_KEY` deve existir somente no backend e nunca deve ser enviada ao frontend ou versionada no Git.

## Upload

Endpoint protegido por `ADMIN`:

```http
POST /api/products/upload-image
Content-Type: multipart/form-data
```

Campo do arquivo:

```text
file
```

Formatos aceitos: JPEG, PNG e WebP.

Limite: 5 MB.

A resposta contém a URL pública e o caminho criado no bucket. A URL pode ser enviada posteriormente em `imageUrl` no cadastro/edição do produto.
