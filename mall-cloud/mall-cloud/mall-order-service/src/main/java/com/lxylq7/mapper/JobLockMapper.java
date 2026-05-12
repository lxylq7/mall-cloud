package com.lxylq7.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface JobLockMapper {

    @Insert("""
        INSERT INTO job_lock (lock_name, locked_until)
        VALUES (#{name}, #{until})
        ON DUPLICATE KEY UPDATE locked_until =
            CASE WHEN locked_until < #{now} THEN #{until} ELSE locked_until END
    """)
    int tryLock(@Param("name") String name,
                @Param("now") LocalDateTime now,
                @Param("until") LocalDateTime until);

    @Update("""
        UPDATE job_lock
        SET locked_until = #{now}
        WHERE lock_name = #{name}
    """)
    int release(@Param("name") String name, @Param("now") LocalDateTime now);
}
