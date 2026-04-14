package br.com.reservasala.api.repository;

import br.com.reservasala.api.models.ZoomAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoomAccountRepository extends JpaRepository<ZoomAccount, Long> {

 
    List<ZoomAccount> findAllByIsActive(boolean isActive);
    
    List<ZoomAccount> findAllByIsActiveAndIsLargeCapacity(boolean isActive, boolean isLargeCapacity);
}
