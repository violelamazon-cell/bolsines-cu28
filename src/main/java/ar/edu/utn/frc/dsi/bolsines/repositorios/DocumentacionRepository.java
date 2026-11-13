package ar.edu.utn.frc.dsi.bolsines.repositorios;

import ar.edu.utn.frc.dsi.bolsines.entidades.Documentacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentacionRepository extends JpaRepository<Documentacion, Long> {
    Optional<Documentacion> findByNumero(int numero);
}

