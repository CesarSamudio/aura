package com.cesar.ahorros.data

/**
 * La lista del salón del 11G.
 *
 * **Esta versión de la app es la del 11G**: trae la lista ya escrita, con los
 * 24 compañeros, para no tener que escribir los nombres a mano cuando se
 * arma una deuda. Viene con el código, así que no se puede borrar ni cambiar
 * (las listas que crea el usuario sí se pueden).
 *
 * La lista original de Cesar saltaba del número 7 al 9 (no estaba el 8).
 * Se dejó tal cual: si falta alguien, se agrega.
 */
object ListaSalon {

    /** La lista del 11G, o `null` en las versiones que no la traen. */
    val lista: ListaDeGente? = ListaDeGente(
        id = "salon11g",
        nombre = "Salón 11G",
        personas = listOf(
            "Kenneth Yassir Alvarado Hurtado",
            "Angel Emanuel Badillo Gallardo",
            "Danna Isabella Delgado Gonzalez",
            "Caleb José Duarte Gracia",
            "Carlos Gilberto Espinosa Villarreal",
            "Erick Leonel Garcia Mayorca Reyes",
            "David Ivan Jiron Tuñon",
            "Nuritz Mileydis Lopez Boyd",
            "Isamar Reichelle Lord Castro",
            "Dreik Adrik Mendez Sanchez",
            "Yonatan Javier Ortega Abrego",
            "Geilys Laritza Ortega Viquez",
            "Jose Daniel Patiño Castillo",
            "Samuel Aaron Pineda",
            "Sollange Alejandra Prias Salazar",
            "Rebeca Esther Quezada Vilchez",
            "Miguel Angel Romero Chavez",
            "Cesar Antonio Samudio Chavarria",
            "Allan Oscar Sanchez Arroyo",
            "Alexis Abdiel Sanchez Martinez",
            "Joseph Ameth Sanchez Moran",
            "Mario Alberto Veliz Aguilar",
            "Jasline Rossmary Villarreal Mendoza",
            "LUIS CALDERON"
        ),
        bloqueada = true
    )
}
