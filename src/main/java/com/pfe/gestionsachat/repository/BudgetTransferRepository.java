package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.BudgetTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BudgetTransferRepository extends JpaRepository<BudgetTransfer, Integer> {
    List<BudgetTransfer> findByDaHeader_OidDa(Integer oidDa);

    @Query("SELECT b FROM BudgetTransfer b WHERE b.daf.oidUser = :oidUser")
    List<BudgetTransfer> findByDaf_OidUser(@Param("oidUser") Integer oidUser);
    
    List<BudgetTransfer> findBySubSource_OidSub(Integer oidSub);
    
    List<BudgetTransfer> findBySubCible_OidSub(Integer oidSub);
}
