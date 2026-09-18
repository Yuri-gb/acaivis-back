INSERT INTO products (name,size,description,price,image_url,badge,stock_quantity,available,category_id)
SELECT v.name,v.size,v.description,v.price,NULL,v.badge,20,TRUE,c.id
FROM (VALUES
('Açaí Tradicional','500 ml','Açaí cremoso e geladinho, perfeito para qualquer momento.',18.90,'MAIS VENDIDO','Açaís'),
('Açaí com Leite','500 ml','Açaí cremoso combinado com leite para deixar tudo ainda mais gostoso.',20.90,'QUERIDINHO','Açaís'),
('Açaí com Morango','500 ml','Açaí geladinho com morangos para uma combinação irresistível.',22.90,'PREMIUM','Açaís'),
('Açaí Tradicional','300 ml','Açaí cremoso e geladinho para matar a vontade de açaí.',12.90,'MAIS VENDIDO','Açaís'),
('Açaí com Leite em Pó','300 ml','Açaí cremoso com leite em pó para deixar o pedido ainda mais saboroso.',14.90,'QUERIDINHO','Açaís'),
('Açaí com Morango','300 ml','Açaí com morangos frescos em uma combinação irresistível.',15.90,'PREMIUM','Açaís'),
('Açaí Tradicional','700 ml','Uma porção maior do nosso açaí tradicional para compartilhar.',27.90,'MAIS VENDIDO','Açaís'),
('Açaí com Leite','700 ml','Açaí cremoso com leite em uma porção maior.',29.90,'QUERIDINHO','Açaís'),
('Açaí com Morango','700 ml','Açaí com morango em uma porção generosa.',31.90,'PREMIUM','Açaís'),
('Açaí Tradicional','1 litro','Açaí tradicional em tamanho família.',36.90,'MAIS VENDIDO','Açaís'),
('Açaí com Leite','1 litro','Açaí com leite em uma porção família.',39.90,'QUERIDINHO','Açaís'),
('Açaí com Morango','1 litro','Açaí com morango em tamanho família.',42.90,'PREMIUM','Açaís'),
('Combo Açaívis','2 garrafas 500 ml','Duas garrafas de açaí para aproveitar em dobro.',32.90,'COMBO','Combos'),
('Combo Leite em Pó','2 garrafas 500 ml','Duas garrafas de açaí com leite para compartilhar.',36.90,'COMBO','Combos'),
('Combo Morango','2 garrafas 500 ml','Duas garrafas de açaí com morango.',39.90,'COMBO','Combos'),
('Açaí Tradicional','2 garrafas 300 ml','Duas garrafas individuais de açaí tradicional.',24.90,NULL,'Combos'),
('Açaí com Leite em Pó','2 garrafas 300 ml','Duas garrafas individuais com leite em pó.',28.90,NULL,'Combos'),
('Açaí com Morango','2 garrafas 300 ml','Duas garrafas individuais com morango.',30.90,NULL,'Combos'),
('Combo Açaívis Família','2 garrafas 1 litro','Duas garrafas de um litro para um pedido em família.',69.90,'COMBO','Combos'),
('Combo Açaívis Morango','2 garrafas 1 litro','Duas garrafas de um litro com a combinação de açaí e morango.',79.90,'COMBO','Combos')
) AS v(name,size,description,price,badge,category_name)
JOIN categories c ON c.name=v.category_name;
