package com.acaivis.model;
import jakarta.persistence.*; import java.math.BigDecimal;
@Entity @Table(name="delivery_zones")
public class DeliveryZone { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String name; @Column(nullable=false,precision=10,scale=2) private BigDecimal fee; @Column(nullable=false) private boolean active=true; public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public BigDecimal getFee(){return fee;} public void setFee(BigDecimal v){fee=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;} }
