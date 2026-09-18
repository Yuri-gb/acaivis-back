package com.acaivis.model;
import jakarta.persistence.*;
@Entity @Table(name="categories")
public class Category { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String name; @Column(nullable=false) private boolean active=true; public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;} }
