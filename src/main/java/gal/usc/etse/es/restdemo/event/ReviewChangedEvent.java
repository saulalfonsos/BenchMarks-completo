package gal.usc.etse.es.restdemo.event;

/** Publícase ao crear, modificar ou borrar unha valoración. */
public record ReviewChangedEvent(Long benchID) {}
