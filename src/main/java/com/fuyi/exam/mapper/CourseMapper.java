package com.fuyi.exam.mapper;

import com.fuyi.exam.entity.Course;
import com.fuyi.exam.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CourseMapper {
    // 查所有上架课程
    @Select("SELECT * FROM sys_course WHERE status = 1 ORDER BY sort_order ASC")
    List<Course> findAllCourses();

    @Select("SELECT * FROM sys_course WHERE id = #{id}")
    Course findCourseById(Integer id);

    // 查某课程下的所有章节
    @Select("SELECT * FROM sys_chapter WHERE course_id = #{courseId} ORDER BY sort_order ASC")
    List<Chapter> findChaptersByCourseId(Integer courseId);
}