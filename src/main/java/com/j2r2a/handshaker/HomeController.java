package com.j2r2a.handshaker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.j2r2a.handshaker.model.Categoria;
import com.j2r2a.handshaker.model.Comentario;
import com.j2r2a.handshaker.model.Negociacion;
import com.j2r2a.handshaker.model.Oferta;
//import com.j2r2a.handshaker.model.OfertaRecibida;
import com.j2r2a.handshaker.model.Servicio;
import com.j2r2a.handshaker.model.Usuario;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	@PersistenceContext
	private EntityManager entityManager;
	
	/**
	 * Intercepts login requests generated by the header; then continues to load normal page
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	@Transactional
	public String login(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam("name") String formName,@RequestParam("pass") String formPass,
			@RequestParam("source") String formSource) {
		
		logger.info("Login attempt from '{}' while visiting '{}'", formName, formSource);
				
		// validate request	
		
		if (formName==null || formPass==null || formName.length() < 5 || formPass.length() < 5) {			
			session.setAttribute("loginError", "Usuario y/o contraseña: 5 caracteres minimo");
		}
		else {
			
			Usuario u = null;
			
			try {
				
				u = (Usuario)entityManager.createNamedQuery("ExisteUsuarioLogin").setParameter("UsuarioMetido", formName).getSingleResult();
				
				if (u.isPassValid(formPass)) {					
					logger.info("pass was valid");						
					session.setAttribute("usuario", u);					
					// sets the anti-csrf token
					getTokenForSession(session);					
						if(u.getRol().equalsIgnoreCase("administrador")){ 
							return "redirect:" + "administrador";
						}	
					if(formSource.equalsIgnoreCase("/mi_perfil/Usuario/0")){
						
						return "redirect:" + "mi_perfil/Usuario/"+u.getId();
					}
				} else {					
					logger.info("pass was NOT valid");
					session.setAttribute("loginError", "Error en usuario o contraseña");				
				}				
			}
			catch (NoResultException nre) {				
				logger.info("no such login: {}", formName);				
				session.setAttribute("loginError", "No existe el usuario introducido");				
			}
		}	
		return "redirect:" + formSource;		
	}
	
	/**
	 * Logout (also returns to home view).
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String logout(HttpSession session) {
		logger.info("User '{}' logged out", session.getAttribute("usuario"));
		session.invalidate();
		return "redirect:/";
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/registro", method = RequestMethod.GET)
	public String registroHome(HttpServletRequest request,Locale locale, Model model) {
		
		
		return "registro";
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/nuevoRegistro", method = RequestMethod.POST)
	@Transactional
	
	public String nuevoRegistroForm(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam("fotoRegistro") MultipartFile formFotoRegistro,@RequestParam("AliasRegistro") String formAliasRegistro,
			@RequestParam("NombreRegistro") String formNombreRegistro,@RequestParam("EdadRegistro") Long formEdadRegistro,
			@RequestParam("EmailRegistro") String formEmailRegistro,@RequestParam("ContraseniaRegistro") String formContrasenia1Registro,
			@RequestParam("Contrasenia2Registro") String formContrasenia2Registro,@RequestParam("lat") double formLatitudRegistro,
			@RequestParam("lng") double formLongitudRegistro,@RequestParam("servs") String habilidades_metidas,
			@RequestParam("intereses") String intereses_metidos) {
					
		List<Usuario> lista_usuarios = entityManager.createQuery("select u from Usuario u").getResultList();
		int contadorUsuarios = lista_usuarios.size();
		String id_usuario = String.valueOf(contadorUsuarios+1).toString();
		
		if(formContrasenia1Registro.equals(formContrasenia2Registro)){					
			if (!formFotoRegistro.isEmpty()) {
					
				try{					          
	                byte[] bytes = formFotoRegistro.getBytes();
	                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(ContextInitializer.getFile("usuario", id_usuario)));
	                stream.write(bytes);
	                stream.close();	                
	                logger.info( "You successfully uploaded " + id_usuario + 
	               		" into " + ContextInitializer.getFile("usuario", id_usuario).getAbsolutePath() + "!");														
				}
				catch (Exception e) {
					// TODO: handle exception
					logger.info("You failed to upload " + id_usuario + " => " + e.getMessage());
				}				
			}	
			else{								
				logger.info( "El usuario con id " + id_usuario + "no ha elegido ninguna foto");				
			}
						
		Usuario user = Usuario.crearUsuario(formAliasRegistro, formNombreRegistro,"usuario",formEdadRegistro, formEmailRegistro, formContrasenia1Registro,formLatitudRegistro,formLongitudRegistro);								
		List<Servicio> lista_habilidades = new ArrayList<Servicio>();		
		String aux = habilidades_metidas.replaceAll("[^0-9]+","");
			
			for(int i=0; i < aux.length();i++){			
				long id_serv =(long)(aux.charAt(i)-'0');				
				Servicio s = (Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", id_serv).getSingleResult();				
				s.setContadorUsuarios(s.getContadorUsuarios()+1);
				lista_habilidades.add(s);								
			}			
		user.setHabilidades(lista_habilidades);
			
		List<Servicio> lista_intereses = new ArrayList<Servicio>();		
		String auxInteres = intereses_metidos.replaceAll("[^0-9]+","");
			
			for(int i=0; i < auxInteres.length();i++){				
				long id_serv =(long)(auxInteres.charAt(i)-'0');				
				Servicio s = (Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", id_serv).getSingleResult();
				lista_intereses.add(s);								
			}			
		user.setIntereses(lista_intereses);
						
		entityManager.persist(user);				
		session.setAttribute("usuario", user);			
		user.printUsuario();
				
		}
							
		return "redirect:" +"index";
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/anadirNuevoServicio", method = RequestMethod.POST)
	@Transactional
	
	public String anadirNuevoServicioForm(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam("tituloServicio") String formTituloServicio,@RequestParam("categoriaServicio") long formCategoriaServicio,
			@RequestParam("descripcionServicio") String formDescripcionServicio) {
		
		Usuario usuario = (Usuario)session.getAttribute("usuario");
		
		long id = usuario.getId();
		
		Categoria categoria = (Categoria)entityManager.createNamedQuery("ExisteCategoriaPorID").setParameter("IDCategoriaMetida", formCategoriaServicio).getSingleResult();
		
		Servicio servicio = Servicio.crearServicio(formTituloServicio, categoria, formDescripcionServicio,0);
		entityManager.persist(servicio);
			
		return "redirect:"+ "mi_perfil/Usuario/"+id;
	
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/anadirNuevaHabilidad", method = RequestMethod.POST)
	@Transactional
	
	public String anadirNuevaHabilidadForm(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam("servs") String habilidades_metidas) {
		
		Usuario usuario = (Usuario)session.getAttribute("usuario");	
		long id = usuario.getId();
		
		List<Servicio> lista_habilidades = entityManager.createQuery("SELECT DISTINCT u.habilidades FROM Usuario u WHERE u.id="+id+"").getResultList();
		String aux = habilidades_metidas.replaceAll("[^0-9]+","");
			
			for(int i=0; i < aux.length();i++){			
				long id_serv =(long)(aux.charAt(i)-'0');				
				Servicio s = (Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", id_serv).getSingleResult();				
				s.setContadorUsuarios(s.getContadorUsuarios()+1);
				lista_habilidades.add(s);
				//entityManager.createQuery("INSERT into habilidadesUsuario("+id +','+s.getId_servicio()+')');
			}			
		usuario.setHabilidades(lista_habilidades);
		
		entityManager.merge(usuario);
					
		return "redirect:"+ "mi_perfil/Usuario/"+id;
	
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/anadirNuevoInteres", method = RequestMethod.POST)
	@Transactional
	
	public String anadirNuevoInteresForm(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam("intereses") String intereses_metidos) {
		
		Usuario usuario = (Usuario)session.getAttribute("usuario");	
		long id = usuario.getId();
		
		List<Servicio> lista_intereses = entityManager.createQuery("SELECT DISTINCT u.intereses FROM Usuario u WHERE u.id="+id+"").getResultList();		
		String auxInteres = intereses_metidos.replaceAll("[^0-9]+","");
			
			for(int i=0; i < auxInteres.length();i++){				
				long id_serv =(long)(auxInteres.charAt(i)-'0');				
				Servicio s = (Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", id_serv).getSingleResult();
				lista_intereses.add(s);								
			}			
		usuario.setIntereses(lista_intereses);
		
		entityManager.merge(usuario);
					
		return "redirect:"+ "mi_perfil/Usuario/"+id;
	
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/mi_perfil/Usuario/{id}", method = RequestMethod.GET)
	public String mi_perfilHome(HttpServletRequest request,Model model,HttpSession session,
			@PathVariable("id") long idUsuarioPulsado) {
								
		if(idUsuarioPulsado==0){			
			Usuario u = null;
		}		
		else{
		
			Usuario u = (Usuario)entityManager.createNamedQuery("ExisteUsuarioPorID").setParameter("IDMetido", idUsuarioPulsado).getSingleResult();
						
			if(u!=null){			
				model.addAttribute("usuarioPerfil", u);				
				List<Servicio> listaServiciosUsuario= entityManager.createQuery("SELECT DISTINCT u.habilidades from Usuario u join u.habilidades h where u.id = "+ idUsuarioPulsado +"").getResultList();
				
					if(listaServiciosUsuario!=null){
						model.addAttribute("listaServiciosUsuario",listaServiciosUsuario);
					}			
				List<Servicio> listaInteresesUsuario= entityManager.createQuery("SELECT DISTINCT u.intereses from Usuario u join u.intereses h where u.id = "+ idUsuarioPulsado +"").getResultList();				
					if(listaInteresesUsuario!=null){
						model.addAttribute("listaInteresesUsuario",listaInteresesUsuario);
					}
			}
		}
		
		List<Categoria> listaTodasCategorias = entityManager.createNamedQuery("ListaCategorias").getResultList();
		model.addAttribute("listaCategorias", listaTodasCategorias);
		
		model.addAttribute("elemNavbarActive2","class='active'");
		
		model.addAttribute("prefix", "../../");
		
		return "mi_perfil";
	}
	
	
	/**
		 * Returns a users' photo
		 * @param id id of user to get photo from
		 * @return
	 */
	@ResponseBody
	@RequestMapping(value="/mi_perfil/usuario", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	 public byte[] userPhoto(HttpServletRequest request,HttpSession session,@RequestParam("id_usuario") long iDusuario) throws IOException {
					
			Usuario u = (Usuario)entityManager.createNamedQuery("ExisteUsuarioPorID").setParameter("IDMetido", iDusuario).getSingleResult();			
		    File f = ContextInitializer.getFile("usuario", String.valueOf(u.getId()).toString());
		    InputStream in = null;
		    if (f.exists()) {
		    	in = new BufferedInputStream(new FileInputStream(f));
		    } else {
		    	in = new BufferedInputStream(
		    			this.getClass().getClassLoader().getResourceAsStream("unknown-user.jpg"));
		    }
		    
		    return IOUtils.toByteArray(in);
		}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String index(Model model) {
				
		List<Categoria> listaCategorias = entityManager.createNamedQuery("ListaCategorias").getResultList();
		model.addAttribute("listaCategorias", listaCategorias);
		
		List<Servicio> lista_servicios_todas = entityManager.createNamedQuery("ListarTodo").getResultList();
		model.addAttribute("ListarPorCategoria", lista_servicios_todas);
		
		model.addAttribute("elemNavbarActive1","class='active'");
		
		return "index";
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/index", method = RequestMethod.GET)
	public String indexHome(Model model) {
			
		List<Categoria> listaCategorias = entityManager.createNamedQuery("ListaCategorias").getResultList();
		model.addAttribute("listaCategorias", listaCategorias);
		
		List<Servicio> lista_servicios_todas = entityManager.createQuery("select s from Servicio s").getResultList();
		model.addAttribute("ListarPorCategoria", lista_servicios_todas);
		
		model.addAttribute("elemNavbarActive1","class='active'");
		
		return "index";
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/busquedaIndex", method = RequestMethod.POST)
	@Transactional
	
	public String busquedaIndexForm(HttpServletRequest request, Model model, HttpSession session,
			@RequestParam("textoBuscado") String formTextoBuscado,@RequestParam("categoria") String formCategoria) {
			
		long formCategoriaSeleccionada=1;		
		if(formCategoria.equals("-- Selecciona --")){			
			formCategoriaSeleccionada=1;
		}
		else{		
			formCategoriaSeleccionada= Long.parseLong(request.getParameter("categoria"));		
		}
		
		if(!formTextoBuscado.equals("")){			
			if(formCategoriaSeleccionada==1){
				List<Servicio> lista_servicios_todas = entityManager.createNamedQuery("BusquedaServicioPorSoloTexto").setParameter("textoMetido", "%" + formTextoBuscado + "%").getResultList();
				model.addAttribute("ListarPorCategoria", lista_servicios_todas);
			}
			else{
				List<Servicio> lista_servicios_todas = entityManager.createNamedQuery("BusquedaServicioPorTextoYCategoria").setParameter("textoMetido", "%" + formTextoBuscado + "%").setParameter("categoriaMetida", formCategoriaSeleccionada).getResultList();
				model.addAttribute("ListarPorCategoria", lista_servicios_todas);
			}
		}		
		else{	
			if(formCategoriaSeleccionada==1){				
				List<Servicio> lista_servicios_todas = entityManager.createQuery("select s from Servicio s").getResultList();
				model.addAttribute("ListarPorCategoria", lista_servicios_todas);
			}
			else{			
				List<Servicio> lista_servicios_buscadas = entityManager.createNamedQuery("BusquedaPorCategoria").setParameter("CategoriaMetida",formCategoriaSeleccionada).getResultList();
				model.addAttribute("ListarPorCategoria", lista_servicios_buscadas);			
			}
			
		}
		return "resultadosBusqueda";
	}
	
	@RequestMapping(value = "/dameServicios", method = RequestMethod.POST)
	@Transactional // needed to allow lazy init to work
	
	public ResponseEntity<String> dameServicios(HttpServletRequest request) {
		
		try {			
			List<Categoria> listaCategorias = entityManager.createNamedQuery("ListaCategorias").getResultList();			
			StringBuilder sb = new StringBuilder("[");
			
			for(int i=0; i < listaCategorias.size();i++){
				
				if (sb.length()>1) sb.append(",");				
				sb.append("{ "
						+ "\"id\": \"" + listaCategorias.get(i).getId_categoria() + "\", "
						+ "\"nombre\": \"" + listaCategorias.get(i).getNombreCategoria() + "\", "
						+ "\"valores\":");
								
				List<Servicio> listaServicios = entityManager.createQuery("select s from Servicio s").getResultList();					
				StringBuilder sb2 = new StringBuilder("[");
				
				for(int j=0;j < listaServicios.size();j++){					
					if(listaCategorias.get(i).getId_categoria()==1){
						
						if (sb2.length()>1) sb2.append(",");
						sb2.append("{ "
								+ "\"id\": \"" + listaServicios.get(j).getId_servicio() + "\", "
								+ "\"nombre\": \"" + listaServicios.get(j).getNombre() + "\"}");
					}					
					else{					
						if(listaServicios.get(j).getCategoria().getId_categoria() == listaCategorias.get(i).getId_categoria()){
						
							if (sb2.length()>1) sb2.append(",");
							sb2.append("{ "
									+ "\"id\": \"" + listaServicios.get(j).getId_servicio() + "\", "
									+ "\"nombre\": \"" + listaServicios.get(j).getNombre() + "\"}");
						}
					}
				}
				sb2.append("]");				
				sb.append(sb2);				
				sb.append("}");
			}			
			logger.info(sb + "]");			
			return new ResponseEntity<String>(sb + "]", HttpStatus.OK);
			
		} catch (NoResultException nre) {
			logger.error("No existen servicios o categorias", nre);
		}
		
		return new ResponseEntity<String>("Error: No existen categorias o servicios de alguna categoria", HttpStatus.BAD_REQUEST);		
	}			
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/administrador", method = RequestMethod.GET)
	public String administradorHome(Model model, HttpSession session) {
		
        Usuario u = (Usuario)session.getAttribute("usuario");
        
		if(u!=null && u.getRol().equalsIgnoreCase("administrador")){			
				
			List<Usuario> lista_usuarios = entityManager.createQuery("select u from Usuario u").getResultList();
			List<Servicio> lista_servicios = entityManager.createNamedQuery("ListarTodo").getResultList();
			List<Negociacion> lista_negociacion = entityManager.createNamedQuery("DameListaNegociacion").getResultList();
			model.addAttribute("lista_todas_negociaciones",lista_negociacion);
			model.addAttribute("lista_todos_usuarios", lista_usuarios);
			model.addAttribute("lista_todos_servicios", lista_servicios);
			model.addAttribute("elemNavbarActive5","class='active'");		
		}		
		return "administrador";
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/mi_historial/Usuario/{id}", method = RequestMethod.GET)
	 public String mi_historialHome(Model model, HttpSession session,HttpServletRequest request,
			 @PathVariable("id") long IdUsuarioPulsado) {
		if(IdUsuarioPulsado==0){
			Usuario u=null;
		}
		else{
			Usuario u = (Usuario)entityManager.createNamedQuery("ExisteUsuarioPorID").setParameter("IDMetido", IdUsuarioPulsado).getSingleResult();
			List<Oferta> listaOfertasAceptadas = entityManager.createNamedQuery("ListaOfertasAceptadas").setParameter("UsuarioMetido", u).getResultList();
	
	
			if(listaOfertasAceptadas.size() !=0){
				model.addAttribute("listaOfertasAceptadas",listaOfertasAceptadas);
			}
	
		}
	
		model.addAttribute("elemNavbarActive3","class='active'");
	
		model.addAttribute("prefix", "../../");
		return "mi_historial";
	
	 }
	
	
	/**
	 * Checks the anti-csrf token for a session against a value
	 * @param session
	 * @param token
	 * @return the token
	 */
	static boolean isTokenValid(HttpSession session, String token) {
	    Object t=session.getAttribute("csrf_token");
	    return (t != null) && t.equals(token);
	}
	
	/**
	 * Returns an anti-csrf token for a session, and stores it in the session
	 * @param session
	 * @return
	 */
	static String getTokenForSession (HttpSession session) {
	    String token=UUID.randomUUID().toString();
	    session.setAttribute("csrf_token", token);
	    return token;
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/delUser", method = RequestMethod.POST)
	@ResponseBody
	@Transactional // needed to allow DB change
	public ResponseEntity<String> borrarUsuario(@RequestParam("id") long id,
			@RequestParam("csrf") String token, HttpSession session) {
		
	    if (entityManager.createNamedQuery("delUser")
				.setParameter("idParam", id).executeUpdate() == 1) {
			return new ResponseEntity<String>("Ok: user " + id + " removed", 
					HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Error: no such user", 
					HttpStatus.BAD_REQUEST);
		}
	}			
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/delService", method = RequestMethod.POST)
	@ResponseBody
	@Transactional // needed to allow DB change
	public ResponseEntity<String> borrarServicio(@RequestParam("id") long id,
			@RequestParam("csrf") String token, HttpSession session) {
		
		Servicio s = (Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", id).getSingleResult();
		
		if(entityManager.createNamedQuery("BorrarOfertasPorIDServicio").setParameter("ServicioMetido", s).executeUpdate() >=1){
		
			if(entityManager.createNamedQuery("BorrarServicio").setParameter("IDServicio", s.getId_servicio()).executeUpdate()==1){
				return new ResponseEntity<String>("Ok: service " + id + " removed", HttpStatus.OK);
			}
			
			else{	 
				return new ResponseEntity<String>("Fallo al eliminar servicio: " + id, HttpStatus.BAD_REQUEST);	
			}
		}
		else{
			return new ResponseEntity<String>("Fallo al eliminar servicio: " + id, HttpStatus.BAD_REQUEST);	

		}
	}	
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/delNeg", method = RequestMethod.POST)
	@ResponseBody
	@Transactional // needed to allow DB change
	public ResponseEntity<String> borrarNegociacion(@RequestParam("id") long id,
			@RequestParam("csrf") String token, HttpSession session) {
		
		Negociacion n = (Negociacion)entityManager.createNamedQuery("ExisteNegociacionPorID").setParameter("IdNegociacionMetido", id).getSingleResult();
		
		int a = entityManager.createNamedQuery("BorrarOfertaPorIDnegociacion").setParameter("NegociacionMetida", n).executeUpdate();
		int b = entityManager.createNamedQuery("EliminarNegociacionPorID")
				.setParameter("IdNegociacionMetido", id).executeUpdate();
	    if (b == 1) {
			return new ResponseEntity<String>("Ok: user " + id + " removed", 
					HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Error: no such user", 
					HttpStatus.BAD_REQUEST);
		}
	}		
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/getUser", method = RequestMethod.GET)
	@ResponseBody
	@Transactional // needed to allow DB change
	public ResponseEntity<String> obtenerUsuarioPorID(@RequestParam("csrf") String token,
			@RequestParam("id") long id) {

		//Traigo al usuario con el id a actualizar, si lo encuentra, lo modifico	
		Usuario u = (Usuario)entityManager.createNamedQuery("ExisteUsuarioPorID").setParameter("IDMetido", id).getSingleResult();
								
		
			if(u!=null){
				StringBuilder sb = new StringBuilder();			
				if (sb.length()>1) sb.append(",");				
				sb.append("{"
				+ "\"alias\":\"" + u.getAlias() + "\", " 
				+ "\"edad\":\"" + u.getEdad() + "\", "
				+ "\"email\":\"" + u.getEmail() + "\", "
				+ "\"pass\":\"" + u.getContrasenia() + "\", "
				+ "\"nombre\":\"" + u.getNombre() + "\"}");

				String datos = sb.toString();
				System.err.println(datos);
				
				return new ResponseEntity<String>(datos, HttpStatus.OK);
		} else {
			return new ResponseEntity<String>("Error: no such user", 
					HttpStatus.BAD_REQUEST);
		}
		
		
	}		
	
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/editUser", method = RequestMethod.POST)
	@Transactional // needed to allow DB change
	public String editarUsuario(HttpServletRequest request,
			@RequestParam("id") long id,
			@RequestParam("alias_nuevo") String alias,
			@RequestParam("nombre_nuevo") String nombre,
			@RequestParam("edad_nuevo") long edad,
			@RequestParam("email_nuevo") String email,
			@RequestParam("pass1_nuevo") String pass1,
			@RequestParam("pass2_nuevo") String pass2,
			
			//@RequestParam("lat_nuevo") double lat,
			//@RequestParam("lng_nuevo") double lng,
			 HttpSession session) {

		//Traigo al usuario con el id a actualizar, si lo encuentra, lo modifico
		Usuario user = (Usuario)entityManager.createNamedQuery("ExisteUsuarioPorID").setParameter("IDMetido", id).getSingleResult();
		
		
	    if (user!=null)
		{
	    	user.setAlias(alias);
	    	user.setNombre(nombre);
	    	user.setEdad(edad);
	    	user.setEmail(email);
	    	//Si las dos contrase�as son iguales(se han modificado) las grabo, sino las dejo como est�n
	    	if(pass1 == pass2)
	    	user.setContrasenia(pass1);
	    	/*
	    	user.setLatitud(lat);
	    	user.setLongitud(lng);*/
	    
		}	
	    return "redirect:" + "administrador";
	}		
	
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/mis_ofertas", method = RequestMethod.GET)
	public String mis_ofertasHome(Model model, HttpSession session) {
		
		Usuario u = (Usuario)session.getAttribute("usuario");
		
		if(u!=null){
			//model.addAttribute("usuario", u);
			
			List<Oferta> listaOfertasEnviadasUsuario= entityManager.createNamedQuery("ListaOfertaEnviadaUsuario").setParameter("UsuarioMetido", u).getResultList();
			List<Oferta> listaOfertasRecibidasUsuario= entityManager.createNamedQuery("ListaOfertaRecibidaUsuario").setParameter("UsuarioMetido", u).getResultList();
			if(listaOfertasEnviadasUsuario.size() !=0){
				model.addAttribute("listaOfertasEnviadasUsuario",listaOfertasEnviadasUsuario);
			}
			if(listaOfertasRecibidasUsuario.size() !=0){
				model.addAttribute("listaOfertasRecibidasUsuario",listaOfertasRecibidasUsuario);
			}
			long contOfertasEnviadas=(Long)entityManager.createNamedQuery("ContadorOfertasEnviadasUsuario").setParameter("UsuarioMetido", u).getSingleResult();
			long contOfertasRecibidas=(Long)entityManager.createNamedQuery("ContadorOfertasRecibidasUsuario").setParameter("UsuarioMetido", u).getSingleResult();
			model.addAttribute("contadorOfertasEnviadas",contOfertasEnviadas);
			model.addAttribute("contadorOfertasRecibidas",contOfertasRecibidas);
		}			
		
		model.addAttribute("listaActiva4","class='active'");
	
		return "mis_ofertas";
	}
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/negociacion/{id}", method = RequestMethod.GET)
	@Transactional
	public String negociacionHome(Model model, HttpSession session,HttpServletRequest request,
			@PathVariable("id") long IdNegociacionPulsada) {
		
		Negociacion negociacion = (Negociacion)entityManager.createNamedQuery("ExisteNegociacionPorID").setParameter("IdNegociacionMetido", IdNegociacionPulsada).getSingleResult();
		session.setAttribute("negociacion", negociacion);
		
		Oferta oferta = (Oferta)entityManager.createNamedQuery("OfertaPorIDnegociacion").setParameter("IDNegociacion", IdNegociacionPulsada).getSingleResult();
		model.addAttribute("oferta",oferta);
		
		List<Comentario> listaComentarios = entityManager.createNamedQuery("DameListaComentariosPorIDNegociacion").setParameter("IdNegociacionMetido", IdNegociacionPulsada).getResultList();
		
		if(listaComentarios.size()==0){
			model.addAttribute("NoHayComentarios","No hay comentarios en esta negociación. Escriba uno si lo desea");
		}
		else{
			
			model.addAttribute("ListaComentarios",listaComentarios);
		}
		
		model.addAttribute("prefix", "../");

		return "negociacion";
	}
	
	@RequestMapping(value = "/anadirComentarioNegociacion", method = RequestMethod.POST)
	@Transactional
	
	public String anadirComentarioNegociacionHome(Model model, HttpSession session,
			@RequestParam("idNegociacion") long idNegociacion,@RequestParam("textoComentario") String textoComentario){
		
		Usuario u = (Usuario)session.getAttribute("usuario");
		Negociacion negociacion = (Negociacion)entityManager.createNamedQuery("ExisteNegociacionPorID").setParameter("IdNegociacionMetido", idNegociacion).getSingleResult();
					
		if(negociacion != null){

			List<Comentario> listaComentarios = entityManager.createNamedQuery("DameListaComentariosPorIDNegociacion").setParameter("IdNegociacionMetido", idNegociacion).getResultList();
			Date fecha =new Date();
			Comentario c = Comentario.crearComentario(u, textoComentario, negociacion,fecha);
			listaComentarios.add(c);
			entityManager.persist(c);
			negociacion.setLista_comentarios(listaComentarios);
			entityManager.merge(negociacion);
			
			model.addAttribute("ListaComentarios",listaComentarios);
			
		}
		
		 
		
		model.addAttribute("prefix", "../");
				
		return "resultadosChatNegociacion";
	}
	
	//negociacionAceptada	
	@RequestMapping(value = "/negociacionAceptada", method = RequestMethod.POST)
	@Transactional
	public ResponseEntity<String> negociacionAceptadaHome(Model model, HttpSession session,
			@RequestParam("IDNegociacion") long idNegociacion){
		
		Negociacion negociacion = (Negociacion)entityManager.createNamedQuery("ExisteNegociacionPorID").setParameter("IdNegociacionMetido", idNegociacion).getSingleResult();
		negociacion.setAceptada(true);
		session.setAttribute("negociacion", negociacion);
		entityManager.merge(negociacion);
					
		return new ResponseEntity<String>("Negociacion con ID:"+ negociacion.getId_negociacion() +" Aceptada",HttpStatus.OK);	
		
	}
	
		//negociacionCancelada	
		@RequestMapping(value = "/negociacionCancelada", method = RequestMethod.POST)
		@Transactional
		public ResponseEntity<String> negociacionCanceladaHome(Model model, HttpSession session,
				@RequestParam("IDNegociacion") long idNegociacion){
				
			Negociacion negociacion = (Negociacion)entityManager.createNamedQuery("ExisteNegociacionPorID").setParameter("IdNegociacionMetido", idNegociacion).getSingleResult();
			Oferta oferta = (Oferta)entityManager.createNamedQuery("OfertaPorIDnegociacion").setParameter("IDNegociacion", idNegociacion).getSingleResult();
			entityManager.remove(oferta);
			entityManager.remove(negociacion);
														
			return new ResponseEntity<String>("Negociacion con ID:"+ negociacion.getId_negociacion() +" cancelada",HttpStatus.OK);
		}
	
	
	/**
	 * Simply selects the home view to render by returning its name.
	 */
	@RequestMapping(value = "/servicio/{id}", method = RequestMethod.GET)
	public String servicioHome(HttpServletRequest request, Model model, HttpSession session,
			@PathVariable("id") long id_servicio_pulsado) {
				
		Servicio s=(Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", id_servicio_pulsado).getSingleResult();
		Usuario u = (Usuario)session.getAttribute("usuario");		
		
		model.addAttribute("servicio", s);
		
		List<Usuario> listaUsuarios=entityManager.createNamedQuery("ListaUsuariosServicio").setParameter("IdServicioMetido", id_servicio_pulsado).setParameter("idUsuarioMetido", u.getId()).getResultList();				
		if(listaUsuarios!=null){
			model.addAttribute("listaUsuariosServicio",listaUsuarios);
		}
					
		List<Servicio> listaServiciosDeUsuario = entityManager.createQuery("SELECT DISTINCT u.habilidades from Usuario u join u.habilidades h where u.id = "+ u.getId() +"").getResultList();
		if(listaServiciosDeUsuario != null){
			
			model.addAttribute("listaServiciosDeUsuario", listaServiciosDeUsuario);
		}
		
		model.addAttribute("prefix", "../");
		
		return "servicio";
	}
	
	@RequestMapping(value = "/enviaOferta", method = RequestMethod.POST)
	@ResponseBody
	@Transactional // needed to allow DB change
	public ResponseEntity<String> EnviarOferta(HttpSession session,@RequestParam("sSolicita") long sSolicita,
			@RequestParam("sOfrece") long sOfrece,@RequestParam("uEnvia") long uEnvia, @RequestParam("uRecibe") long uRecibe) {
			
			Usuario usuarioSesion=(Usuario)session.getAttribute("usuario");	
	    	Usuario usuarioEnvia=(Usuario)entityManager.createNamedQuery("ExisteUsuarioPorID").setParameter("IDMetido", uEnvia).getSingleResult();
	    	Usuario usuarioRecibe=(Usuario)entityManager.createNamedQuery("ExisteUsuarioPorID").setParameter("IDMetido", uRecibe).getSingleResult();
	    	
	    	Servicio servicioSolicitado=(Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", sSolicita).getSingleResult();
	    	Servicio servicioOfrecido=(Servicio)entityManager.createNamedQuery("ExisteServicioPorNombre").setParameter("IdServicioMetido", sOfrece).getSingleResult();

	    	if(usuarioEnvia!=null && usuarioRecibe!=null && servicioSolicitado!=null && servicioOfrecido!=null && usuarioEnvia.getId()==usuarioSesion.getId()){   		
					
	    			Negociacion negociacion= Negociacion.crearNegociacion(usuarioEnvia, usuarioRecibe, false);
					entityManager.persist(negociacion);
					
					Oferta oferta = Oferta.crearOferta(servicioSolicitado, servicioOfrecido, usuarioEnvia, usuarioRecibe, negociacion);
					entityManager.persist(oferta);
	    	
	    		return new ResponseEntity<String>("Oferta con ID: " + oferta.getId_oferta_enviada() + " realizada con exito",HttpStatus.OK);
	    	}
	    	else{	    		
	    		return new ResponseEntity<String>("Oferta cancelada",HttpStatus.BAD_REQUEST);
	    	}				
	}	
	
	@RequestMapping(value = "/actualizarOfertasRecibidas", method = RequestMethod.POST)
	@Transactional // needed to allow lazy init to work
	
	public ResponseEntity<String> actualizarOfertasRecibidas(HttpServletRequest request,HttpSession session) {
					
			Usuario usuario = (Usuario)session.getAttribute("usuario");
			StringBuilder sb = new StringBuilder("[");
			if(usuario!=null){
				
				long contadorOfertasRecibidas=(Long)entityManager.createNamedQuery("ContadorOfertasRecibidasUsuario").setParameter("UsuarioMetido", usuario).getSingleResult();
				//List<Oferta> listaOfertasRecibidasUsuario= entityManager.createNamedQuery("ListaOfertaRecibidaUsuario").setParameter("UsuarioMetido", usuario).getResultList();
				//int contadorOfertasRecibidas = listaOfertasRecibidasUsuario.size();
				
				if(contadorOfertasRecibidas==0){
					sb.append("{ "+ "\"contador\": \"" + 0 + "\"");											
					sb.append("}");
					return new ResponseEntity<String>(sb + "]", HttpStatus.BAD_REQUEST);		
				}
				else{						
					sb.append("{ "+ "\"contador\": \"" + contadorOfertasRecibidas + "\"");											
					sb.append("}");
					return new ResponseEntity<String>(sb + "]", HttpStatus.OK);
				}
			}
			else{
				sb.append("{ "+ "\"contador\": \"" + 0 + "\"");											
				sb.append("}");
				return new ResponseEntity<String>(sb + "]", HttpStatus.OK);
			}
	}
}
