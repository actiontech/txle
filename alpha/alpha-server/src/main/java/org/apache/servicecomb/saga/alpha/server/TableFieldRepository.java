package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.TableFieldEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TableFieldRepository extends CrudRepository<TableFieldEntity, Long> {

    @Query("FROM TableFieldEntity T WHERE t.tablename = ?1")
    List<TableFieldEntity> selectTableFieldsByName(String tableName);

}
