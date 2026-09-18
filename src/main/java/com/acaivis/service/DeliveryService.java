package com.acaivis.service;

import com.acaivis.dto.*;
import com.acaivis.exception.*;
import com.acaivis.model.*;
import com.acaivis.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal; import java.security.Principal; import java.time.LocalDateTime; import java.text.Normalizer; import java.util.*;
import org.springframework.web.client.RestClient; import org.springframework.web.client.RestClientException;

@Service
public class DeliveryService {
    private static final String VIA_CEP_URL="https://viacep.com.br/ws";
    private static final String FEIRA_DE_SANTANA="feira de santana"; private static final String BAHIA="ba"; private static final String FEIRA_IBGE="2910800";
    private final DeliveryZoneRepository zones; private final OrderRepository orders; private final SupabaseStorageService storage; private final OrderStatusHistoryRepository history; private final RestClient restClient;
    public DeliveryService(DeliveryZoneRepository z, OrderRepository o, SupabaseStorageService s, OrderStatusHistoryRepository h){zones=z;orders=o;storage=s;history=h;restClient=RestClient.builder().baseUrl(VIA_CEP_URL).build();}

    public DeliveryCalculationResponse calculate(String zipCode){
        String normalized=normalizeZipCode(zipCode); final ViaCepResponse address;
        try{address=restClient.get().uri("/{cep}/json/",normalized).retrieve().body(ViaCepResponse.class);}catch(RestClientException e){throw new BusinessException("Não foi possível consultar o CEP no momento");}
        if(address==null||Boolean.TRUE.equals(address.erro()))throw new ResourceNotFoundException("CEP não encontrado"); validateFeiraDeSantana(address); DeliveryZone zone=findZone(address.bairro());
        return new DeliveryCalculationResponse(normalizeZipCode(address.cep()),safe(address.logradouro()),safe(address.bairro()),safe(address.localidade()),safe(address.uf()).toUpperCase(Locale.ROOT),safe(address.ibge()),zone.getId(),zone.getName(),zone.getFee());
    }

    @Transactional(readOnly=true)
    public List<DeliveryOrderResponse> orders(){
        return orders.findAllByStatusInOrderByDeliveryRouteOrderAscCreatedAtAsc(List.of(OrderStatus.READY,OrderStatus.OUT_FOR_DELIVERY)).stream().map(this::toDelivery).toList();
    }

    @Transactional
    public DeliveryOrderResponse setRouteOrder(Long id,Integer routeOrder){
        if(routeOrder==null||routeOrder<1)throw new BusinessException("Ordem de rota inválida");
        Order o=get(id); if(o.getStatus()!=OrderStatus.READY&&o.getStatus()!=OrderStatus.OUT_FOR_DELIVERY)throw new BusinessException("Somente pedidos prontos ou em rota podem entrar na ordem de entrega");
        o.setDeliveryRouteOrder(routeOrder); return toDelivery(orders.save(o));
    }

    @Transactional
    public DeliveryOrderResponse deliver(Long id, MultipartFile proof, Principal principal){
        Order o=get(id);
        if(o.getStatus()!=OrderStatus.READY&&o.getStatus()!=OrderStatus.OUT_FOR_DELIVERY)throw new BusinessException("Este pedido ainda não está liberado para entrega");
        if(proof==null||proof.isEmpty())throw new BusinessException("A foto da entrega é obrigatória");
        SupabaseStorageService.UploadedImage uploaded=storage.uploadDeliveryProof(proof);
        o.setDeliveryProofUrl(uploaded.imageUrl()); o.setDeliveryProofPath(uploaded.path()); o.setDeliveredAt(LocalDateTime.now()); o.setDeliveredBy(principal==null?null:principal.getName());
        if(o.getPaymentMethod()==PaymentMethod.CASH&&!o.isPaymentConfirmed()){o.setPaymentConfirmed(true);}
        o.setStatus(OrderStatus.DELIVERED);
        Order saved=orders.save(o); history.save(new OrderStatusHistory(saved,OrderStatus.DELIVERED,LocalDateTime.now())); return toDelivery(saved);
    }

    private Order get(Long id){return orders.findById(id).orElseThrow(()->new ResourceNotFoundException("Pedido não encontrado: "+id));}
    private DeliveryOrderResponse toDelivery(Order o){return new DeliveryOrderResponse(o.getId(),o.getTrackingCode(),o.getCustomerName(),o.getCustomerPhone(),address(o),o.getNeighborhood(),o.getCity(),o.getState(),o.getZipCode(),o.getTotal(),o.getPaymentMethod(),o.isPaymentConfirmed(),o.getStatus(),o.getItems().stream().map(i->new OrderItemResponse(i.getProduct().getId(),i.getProductName(),i.getSize(),i.getUnitPrice(),i.getQuantity(),i.getSubtotal())).toList(),o.getDeliveryRouteOrder(),o.getDeliveryProofUrl(),o.getDeliveredAt(),o.getDeliveredBy());}
    private String address(Order o){return o.getStreet()+", "+o.getNumber()+(o.getComplement()!=null?" - "+o.getComplement():"")+", "+o.getNeighborhood()+" - "+o.getCity()+"/"+o.getState()+", "+o.getZipCode();}
    private void validateFeiraDeSantana(ViaCepResponse a){String city=normalizeText(a.localidade()),state=normalizeText(a.uf()),ibge=safe(a.ibge()).trim();if(!FEIRA_DE_SANTANA.equals(city)||!BAHIA.equals(state)||(!ibge.isBlank()&&!FEIRA_IBGE.equals(ibge)))throw new BusinessException("No momento, realizamos entregas somente em Feira de Santana - BA");}
    private DeliveryZone findZone(String neighborhood){String n=normalizeText(neighborhood);return zones.findAll().stream().filter(DeliveryZone::isActive).filter(z->normalizeText(z.getName()).equals(n)||matchesAlias(z.getName(),n)).findFirst().orElseThrow(()->new BusinessException("Ainda não realizamos entregas para este bairro"));}
    private boolean matchesAlias(String zoneName,String n){String z=normalizeText(zoneName);if(z.equals("centro industrial cis sul"))return n.equals("centro industrial subae")||n.equals("cis sul")||n.equals("centro industrial cis sul");return false;}
    private String normalizeZipCode(String v){if(v==null)throw new BusinessException("CEP é obrigatório");String n=v.replaceAll("\\D","");if(!n.matches("\\d{8}"))throw new BusinessException("CEP inválido");return n;}
    private String normalizeText(String v){if(v==null)return "";return Normalizer.normalize(v,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim().replaceAll("\\s+"," ");}
    private String safe(String v){return v==null?"":v;}
}
