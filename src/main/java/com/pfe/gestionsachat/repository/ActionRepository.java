package com.pfe.gestionsachat.repository;

import com.pfe.gestionsachat.model.Action;
import com.pfe.gestionsachat.model.TypeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActionRepository extends JpaRepository<Action, Integer> {
    List<Action> findByDaHeader_OidDa(Integer oidDa);

    @Query("SELECT a FROM Action a WHERE a.user.oidUser = :oidUser")
    List<Action> findByUser_OidUser(@Param("oidUser") Integer oidUser);
    
    List<Action> findByTypeAction(TypeAction typeAction);
}
