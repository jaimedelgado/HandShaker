package com.j2r2a.handshaker.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

@Entity

@NamedQueries({
	
	@NamedQuery(name="ListarTodo",query="SELECT DISTINCT s FROM Servicio s"),
    @NamedQuery(name="BusquedaPorCategoria",query="SELECT s FROM Servicio s JOIN s.categoria sCat WHERE sCat.id_categoria= :CategoriaMetida"),
	@NamedQuery(name="ExisteServicioPorNombre",query="SELECT s FROM Servicio s WHERE s.id_servicio = :IdServicioMetido")
	
})

public class Servicio{
		
	private long id_servicio;	
	private String nombre;

	private Categoria categoria;
	private String descripcion;
	
	private List<Usuario> usuarios1; //Para habilidades
	private List<Usuario> usuarios2; //Para intereses
	
	private long contadorUsuarios;
	
	public Servicio(){}
	
	public static Servicio crearServicio(String nombre,Categoria categoria,String descripcion,long contadorUsuarios){
		
		Servicio serv= new Servicio();
		serv.nombre=nombre;
		serv.setCategoria(categoria);
		serv.descripcion=descripcion;
		serv.contadorUsuarios=contadorUsuarios;
		
		return serv;
	}
	
	@Id
    @GeneratedValue
	public long getId_servicio() {
		return id_servicio;
	}
	public void setId_servicio(long id_servicio) {
		this.id_servicio = id_servicio;
	}
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
			
	public String getDescripcion() {
		return descripcion;
	}
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	
	
	@ManyToMany(targetEntity=Usuario.class, mappedBy="habilidades")
	public List<Usuario> getUsuarios1() {
		return usuarios1;
	}

	public void setUsuarios1(List<Usuario> usuarios) {
		this.usuarios1 = usuarios;
	}
	
	
	@ManyToMany(targetEntity=Usuario.class, mappedBy="intereses")
	public List<Usuario> getUsuarios2() {
		return usuarios2;
	}

	public void setUsuarios2(List<Usuario> usuarios) {
		this.usuarios2 = usuarios;
	}
	
	
	@OneToOne(targetEntity=Categoria.class)
	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

	public long getContadorUsuarios() {
		return contadorUsuarios;
	}

	public void setContadorUsuarios(long contadorUsuarios) {
		this.contadorUsuarios = contadorUsuarios;
	}
	
	
}