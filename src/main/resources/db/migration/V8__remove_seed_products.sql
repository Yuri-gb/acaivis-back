DELETE FROM products
WHERE image_url IS NULL
  AND name IN (
    'Açaí Tradicional',
    'Açaí com Leite',
    'Açaí com Morango',
    'Açaí com Leite em Pó',
    'Combo Açaívis',
    'Combo Leite em Pó',
    'Combo Morango',
    'Combo Açaívis Família',
    'Combo Açaívis Morango'
  )
  AND stock_quantity = 20;