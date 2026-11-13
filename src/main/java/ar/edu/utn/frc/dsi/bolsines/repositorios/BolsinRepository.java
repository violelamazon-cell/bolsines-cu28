package ar.edu.utn.frc.dsi.bolsines.repositorios;

import ar.edu.utn.frc.dsi.bolsines.entidades.Bolsin;
import ar.edu.utn.frc.dsi.bolsines.entidades.ComisionMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BolsinRepository extends JpaRepository<Bolsin, Long> {

    Optional<Bolsin> findByNumeroBolsin(int numeroBolsin);

    Optional<Bolsin> findByNumeroPrecinto(int numeroPrecinto);

    List<Bolsin> findByDestino(ComisionMedica destino);

    /**
     * Trae todos los bolsines destinados a una CM con sus relaciones clave precargadas
     * para evitar LazyInitializationException fuera del contexto transaccional.
     */
    @Query("SELECT DISTINCT b FROM Bolsin b " +
           "LEFT JOIN FETCH b.origen " +
           "LEFT JOIN FETCH b.destino " +
           "LEFT JOIN FETCH b.cambioEstado ce " +
           "LEFT JOIN FETCH ce.estado " +
           "WHERE b.destino = :destino")
    List<Bolsin> findByDestinoWithCambios(@Param("destino") ComisionMedica destino);
}

