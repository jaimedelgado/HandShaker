
    create table Categoria (
        id_categoria bigint not null,
        nombreCategoria varchar(255),
        id_servicio bigint,
        primary key (id_categoria)
    );

    create table Comentario (
        id_comentario bigint generated by default as identity (start with 1),
        texto_comentario varchar(255),
        id_usuario_id bigint,
        negociacion_id_negociacion bigint,
        id_negociacion bigint,
        primary key (id_comentario)
    );

    create table Negociacion (
        id_negociacion bigint generated by default as identity (start with 1),
        aceptada bit not null,
        usuario1_id bigint,
        usuario2_id bigint,
        primary key (id_negociacion)
    );

    create table Oferta_enviada (
        id_oferta_enviada bigint generated by default as identity (start with 1),
        fecha date,
        negociacion_id_negociacion bigint,
        servicio_recibido_id_servicio bigint,
        usuario_id bigint,
        primary key (id_oferta_enviada)
    );

    create table Oferta_recibida (
        id_oferta_recibida bigint generated by default as identity (start with 1),
        negociacion_id_negociacion bigint,
        servicio_recibido_id_servicio_ofrecido bigint,
        usuario_receptor_id bigint,
        primary key (id_oferta_recibida)
    );

    create table Servicio (
        id_servicio bigint generated by default as identity (start with 1),
        descripcion varchar(255),
        nombre varchar(255),
        categoria_id_servicio bigint,
        usuario_id bigint,
        id_usuario_intereses bigint,
        id_usuario_habilidades bigint,
        primary key (id_servicio)
    );

    create table Servicio_ofrecido (
        id_servicio_ofrecido bigint generated by default as identity (start with 1),
        servicio_ofrecido_id_servicio bigint,
        usuario_id bigint,
        id_oferta_enviada bigint,
        primary key (id_servicio_ofrecido)
    );

    create table Usuario (
        id bigint generated by default as identity (start with 1),
        alias varchar(255),
        contrasenia varchar(255),
        edad bigint not null,
        email varchar(255),
        latitud double not null,
        longitud double not null,
        nombre varchar(255),
        salt varchar(255),
        valoracion varbinary(255),
        primary key (id),
        unique (alias)
    );

    alter table Categoria 
        add constraint FKD4C70113B0DAE2C6 
        foreign key (id_servicio) 
        references Servicio;

    alter table Comentario 
        add constraint FK14DFC401A73C6E6 
        foreign key (negociacion_id_negociacion) 
        references Negociacion;

    alter table Comentario 
        add constraint FK14DFC4016EE95F68 
        foreign key (id_usuario_id) 
        references Usuario;

    alter table Comentario 
        add constraint FK14DFC4012BA11528 
        foreign key (id_negociacion) 
        references Negociacion;

    alter table Negociacion 
        add constraint FK9E8E7A21FBF3A20F 
        foreign key (usuario1_id) 
        references Usuario;

    alter table Negociacion 
        add constraint FK9E8E7A21FBF4166E 
        foreign key (usuario2_id) 
        references Usuario;

    alter table Oferta_enviada 
        add constraint FK1F931E14A73C6E6 
        foreign key (negociacion_id_negociacion) 
        references Negociacion;

    alter table Oferta_enviada 
        add constraint FK1F931E14D791ECBB 
        foreign key (servicio_recibido_id_servicio) 
        references Servicio;

    alter table Oferta_enviada 
        add constraint FK1F931E1458E8C5C4 
        foreign key (usuario_id) 
        references Usuario;

    alter table Oferta_recibida 
        add constraint FK1CBC4A8BA73C6E6 
        foreign key (negociacion_id_negociacion) 
        references Negociacion;

    alter table Oferta_recibida 
        add constraint FK1CBC4A8B199061D9 
        foreign key (servicio_recibido_id_servicio_ofrecido) 
        references Servicio_ofrecido;

    alter table Oferta_recibida 
        add constraint FK1CBC4A8B4CC4A065 
        foreign key (usuario_receptor_id) 
        references Usuario;

    alter table Servicio 
        add constraint FK560F74F6A011CCBB 
        foreign key (id_usuario_intereses) 
        references Usuario;

    alter table Servicio 
        add constraint FK560F74F658E8C5C4 
        foreign key (usuario_id) 
        references Usuario;

    alter table Servicio 
        add constraint FK560F74F6DC973D5B 
        foreign key (id_usuario_habilidades) 
        references Usuario;

    alter table Servicio 
        add constraint FK560F74F637ECEDBA 
        foreign key (categoria_id_servicio) 
        references Servicio;

    alter table Servicio_ofrecido 
        add constraint FKFCAF55E458E8C5C4 
        foreign key (usuario_id) 
        references Usuario;

    alter table Servicio_ofrecido 
        add constraint FKFCAF55E411CD710B 
        foreign key (servicio_ofrecido_id_servicio) 
        references Servicio;

    alter table Servicio_ofrecido 
        add constraint FKFCAF55E4A116A982 
        foreign key (id_oferta_enviada) 
        references Oferta_enviada;
