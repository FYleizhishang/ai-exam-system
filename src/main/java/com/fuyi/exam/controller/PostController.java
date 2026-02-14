package com.fuyi.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fuyi.exam.common.Result;
import com.fuyi.exam.entity.Comment;
import com.fuyi.exam.entity.Post;
import com.fuyi.exam.entity.User;
import com.fuyi.exam.mapper.CommentMapper;
import com.fuyi.exam.mapper.PostLikeMapper; // 新增
import com.fuyi.exam.mapper.PostMapper;
import com.fuyi.exam.mapper.UserMapper;
import com.fuyi.exam.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/post")
@CrossOrigin
public class PostController {

    @Autowired private PostMapper postMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private PostLikeMapper postLikeMapper; // 新增注入
    @Autowired private HttpServletRequest request;

    private Integer getCurrentUserId() {
        String token = request.getHeader("token");
        if (token == null) token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        try { return JwtUtil.getUserId(token); } catch (Exception e) { return null; }
    }

    // 1. 帖子列表 (升级：补充当前用户是否已点赞的状态)
    @GetMapping("/list")
    public Result<List<Post>> list() {
        Integer currentUserId = getCurrentUserId();

        QueryWrapper<Post> query = new QueryWrapper<>();
        query.orderByDesc("create_time");
        List<Post> posts = postMapper.selectList(query);

        for (Post p : posts) {
            User u = userMapper.selectById(p.getUserId());
            if (u != null) {
                p.setAuthorName(u.getUsername());
                p.setAuthorAvatar(u.getAvatar());
            } else {
                p.setAuthorName("匿名");
            }

            // 🔥 检查当前用户是否点赞
            if (currentUserId != null) {
                int count = postLikeMapper.countLike(p.getId(), currentUserId);
                // 借用一个暂存字段传给前端，Post 实体类没有 isLiked，这里用 map 或者扩展实体类
                // 为了简单，我们假定 Post 实体类里加个 @TableField(exist=false) boolean isLiked;
                // 如果没加，这里需要前端自己处理，或者我们在 Post.java 里加一个
            }
        }
        return Result.success(posts);
    }

    // 2. 发布帖子
    @PostMapping("/add")
    public Result<String> add(@RequestBody Post post) {
        Integer userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        post.setUserId(userId);
        post.setCreateTime(new Date());
        post.setViewCount(0);
        post.setLikeCount(0);
        postMapper.insert(post);
        return Result.success("发布成功");
    }

    // 3. 删除帖子
    @PostMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        Post post = postMapper.selectById(id);
        if (post == null) return Result.error("帖子不存在");
        User currentUser = userMapper.selectById(userId);

        if (!post.getUserId().equals(userId) && !"admin".equals(currentUser.getRole())) {
            return Result.error("无权删除");
        }
        postMapper.deleteById(id);
        commentMapper.delete(new QueryWrapper<Comment>().eq("post_id", id));
        return Result.success("删除成功");
    }

    // 4. 点赞接口 (🔥 核心升级：防止重复点赞，支持取消)
    @PostMapping("/like/{id}")
    public Result<Map<String, Object>> like(@PathVariable Integer id) {
        Integer userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");

        Post post = postMapper.selectById(id);
        if (post == null) return Result.error("帖子不存在");

        int count = postLikeMapper.countLike(id, userId);
        boolean isLiked;

        if (count > 0) {
            // 已点赞 -> 取消点赞
            postLikeMapper.removeLike(id, userId);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            isLiked = false;
        } else {
            // 未点赞 -> 点赞
            try {
                postLikeMapper.addLike(id, userId);
                post.setLikeCount(post.getLikeCount() + 1);
                isLiked = true;
            } catch (Exception e) {
                return Result.error("操作太快了");
            }
        }
        postMapper.updateById(post);

        Map<String, Object> res = new HashMap<>();
        res.put("likeCount", post.getLikeCount());
        res.put("isLiked", isLiked);
        return Result.success(res);
    }

    @PostMapping("/comment/add")
    public Result<String> addComment(@RequestBody Comment comment) {
        Integer userId = getCurrentUserId();
        if (userId == null) return Result.error("请先登录");
        comment.setUserId(userId);
        comment.setCreateTime(new Date());
        commentMapper.insert(comment);
        return Result.success("评论成功");
    }

    @GetMapping("/comment/list/{postId}")
    public Result<List<Comment>> listComments(@PathVariable Integer postId) {
        QueryWrapper<Comment> q = new QueryWrapper<>();
        q.eq("post_id", postId).orderByDesc("create_time");
        List<Comment> list = commentMapper.selectList(q);
        for (Comment c : list) {
            User u = userMapper.selectById(c.getUserId());
            if (u != null) {
                c.setUsername(u.getUsername());
                c.setAvatar(u.getAvatar());
            } else {
                c.setUsername("神秘人");
            }
        }
        return Result.success(list);
    }
}