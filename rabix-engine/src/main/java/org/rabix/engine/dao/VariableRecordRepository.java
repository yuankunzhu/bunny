package org.rabix.engine.dao;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.postgresql.util.PGobject;
import org.rabix.bindings.model.FileValue;
import org.rabix.bindings.model.LinkMerge;
import org.rabix.bindings.model.dag.DAGLinkPort.LinkPortType;
import org.rabix.common.helper.JSONHelper;
import org.rabix.engine.dao.VariableRecordRepository.VariableRecordMapper;
import org.rabix.engine.model.VariableRecord;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

@RegisterMapper(VariableRecordMapper.class)
public interface VariableRecordRepository {

  @SqlUpdate("insert into variable_record (job_id,value,port_id,type,link_merge,is_wrapped,globals_count,times_updated_count,context_id,is_default,transform) values (:job_id,:value,:port_id,:type,:link_merge,:is_wrapped,:globals_count,:times_updated_count,:context_id,:is_default,:transform)")
  int insert(@BindVariableRecord VariableRecord jobRecord);
  
  @SqlUpdate("update variable_record set job_id=:job_id,value=:value,port_id=:port_id,type=:type,link_merge=:link_merge,is_wrapped=:is_wrapped,globals_count=:globals_count,times_updated_count=:times_updated_count,context_id=:context_id,is_default=:is_default,transform=:transform where port_id=:port_id and context_id=:context_id and job_id=:job_id and type=:type")
  int update(@BindVariableRecord VariableRecord jobRecord);
  
  @SqlQuery("select * from variable_record where job_id=:job_id and port_id=:port_id and type=:type and context_id=:context_id")
  VariableRecord get(@Bind("job_id") String jobId, @Bind("port_id") String portId, @Bind("type") LinkPortType type, @Bind("context_id") String rootId);
 
  @SqlQuery("select * from variable_record where job_id=:job_id and type=:type and context_id=:context_id")
  List<VariableRecord> getByType(@Bind("job_id") String jobId, @Bind("type") LinkPortType type, @Bind("context_id") String rootId);
  
  @SqlQuery("select * from variable_record where job_id=:job_id and port_id=:port_id and context_id=:context_id")
  List<VariableRecord> getByPort(@Bind("job_id") String jobId, @Bind("port_id") String portId, @Bind("context_id") String rootId);
 
  @BindingAnnotation(BindVariableRecord.VariableBinderFactory.class)
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.PARAMETER })
  public static @interface BindVariableRecord {
    public static class VariableBinderFactory implements BinderFactory<Annotation> {
      public Binder<BindVariableRecord, VariableRecord> build(Annotation annotation) {
        return new Binder<BindVariableRecord, VariableRecord>() {
          public void bind(SQLStatement<?> q, BindVariableRecord bind, VariableRecord variableRecord) {
            q.bind("job_id", variableRecord.getJobId());
            
            try {
              PGobject data = new PGobject();
              data.setType("jsonb");
              data.setValue(JSONHelper.writeObject(variableRecord.getValue()));
              q.bind("value", data);
            } catch (SQLException ex) {
              throw new IllegalStateException("Error Binding value", ex);
            }
            
            try {
              PGobject data = new PGobject();
              data.setType("jsonb");
              data.setValue(JSONHelper.writeObject(variableRecord.getTransform()));
              q.bind("transform", data);
            } catch (SQLException ex) {
              throw new IllegalStateException("Error Binding value", ex);
            }
            
            q.bind("port_id", variableRecord.getPortId());
            q.bind("type", variableRecord.getType());
            q.bind("link_merge", variableRecord.getLinkMerge());
            q.bind("is_wrapped", variableRecord.isWrapped());
            q.bind("globals_count", variableRecord.getNumberOfGlobals());
            q.bind("times_updated_count", variableRecord.getNumberOfTimesUpdated());
            q.bind("context_id", variableRecord.getContextId());
            q.bind("is_default", variableRecord.isDefault());
          }
        };
      }
    }
  }
  
  public static class VariableRecordMapper implements ResultSetMapper<VariableRecord> {
    public VariableRecord map(int index, ResultSet resultSet, StatementContext ctx) throws SQLException {
      String jobId = resultSet.getString("job_id");
      String value = resultSet.getString("value");
      String transform = resultSet.getString("transform");
      String portId = resultSet.getString("port_id");
      String type = resultSet.getString("type");
      String linkMerge = resultSet.getString("link_merge");
      Boolean isWrapped = resultSet.getBoolean("is_wrapped");
      Integer globalsCount = resultSet.getInt("globals_count");
      Integer timesUpdatedCount = resultSet.getInt("times_updated_count");
      String contextId = resultSet.getString("context_id");
      Boolean isDefault = resultSet.getBoolean("is_default");

      Object valueObject = FileValue.deserialize(JSONHelper.transform(JSONHelper.readJsonNode(value)));
      
      Object transformObject = JSONHelper.transform(JSONHelper.readJsonNode(transform));
      
      VariableRecord variableRecord = new VariableRecord(contextId, jobId, portId, LinkPortType.valueOf(type), valueObject, LinkMerge.valueOf(linkMerge));
      variableRecord.setWrapped(isWrapped);
      variableRecord.setNumberGlobals(globalsCount);
      variableRecord.setNumberOfTimesUpdated(timesUpdatedCount);
      variableRecord.setDefault(isDefault);
      variableRecord.setTransform(transformObject);
      return variableRecord;
    }
  }
  
}