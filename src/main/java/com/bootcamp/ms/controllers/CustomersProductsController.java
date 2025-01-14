package com.bootcamp.ms.controllers;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.bootcamp.ms.models.documents.Customers;
import com.bootcamp.ms.models.documents.CustomersProducts;
import com.bootcamp.ms.models.services.CustomersProductsService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/customersProducts")
public class CustomersProductsController {
	
	@Autowired
	private CustomersProductsService service;
	
	
	@GetMapping
	public Mono<ResponseEntity<Flux<CustomersProducts>>> lista(){
		return Mono.just(
				ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(service.findAll())
				);
	}
	
	@GetMapping("/{id}")
	public Mono<ResponseEntity<CustomersProducts>> ver(@PathVariable String id){
		return service.findById(id).map(p -> ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(p))
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	
	
	
	@PostMapping
	public Mono<ResponseEntity<Map<String, Object>>> crear(@RequestBody Mono<CustomersProducts> monoClientes){
		
		Map<String, Object> respuesta = new HashMap<String, Object>();
				
		
		return monoClientes.flatMap(ClientesProductos -> {
			if(ClientesProductos.getCreateAt()==null) {
				ClientesProductos.setCreateAt(new Date());
			}
			
			return service.save(ClientesProductos).map(p-> {
				respuesta.put("ClientesProductos", p);
				respuesta.put("mensaje", "Cliente y Producto registrado con éxito");
				respuesta.put("timestamp", new Date());
				return ResponseEntity
					.created(URI.create("/api/customersProducts/".concat(p.getId())))
					.contentType(MediaType.APPLICATION_JSON_UTF8)
					.body(respuesta);
				});
			
		}).onErrorResume(t -> {
			return Mono.just(t).cast(WebExchangeBindException.class)
					.flatMap(e -> Mono.just(e.getFieldErrors()))
					.flatMapMany(Flux::fromIterable)
					.map(fieldError -> "El campo "+fieldError.getField() + " " + fieldError.getDefaultMessage())
					.collectList()
					.flatMap(list -> {
						respuesta.put("errors", list);
						respuesta.put("timestamp", new Date());
						respuesta.put("status", HttpStatus.BAD_REQUEST.value());
						return Mono.just(ResponseEntity.badRequest().body(respuesta));
					});
							
		});
		

	}
	
	@PutMapping("/{id}")
	public Mono<ResponseEntity<CustomersProducts>> editar(@RequestBody CustomersProducts customersProducts, @PathVariable String id){
		
		return service.findById(id).flatMap(p -> {
			p.setClientes(customersProducts.getClientes());
			p.setProductos(customersProducts.getProductos());
			
			
			return service.save(p);
			
		}).map(p->ResponseEntity.created(URI.create("/api/clientesProductos/".concat(p.getId())))
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(p))
		.defaultIfEmpty(ResponseEntity.notFound().build());
		
	}
	
	@DeleteMapping("/{id}")
	public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id){
		return service.findById(id).flatMap(p ->{
			return service.delete(p).then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
		}).defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
	}
	

}
