package com.example.fastcampusmysql.domain.post.repository;

import com.example.fastcampusmysql.util.PageHelper;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCount;
import com.example.fastcampusmysql.domain.post.dto.DailyPostCountRequest;
import com.example.fastcampusmysql.domain.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class PostRepository {
    static final String TABLE = "Post";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final RowMapper<Post> ROW_MAPPER = (ResultSet resultSet, int rowNum) -> Post.builder()
            .id(resultSet.getLong("id"))
            .memberId(resultSet.getLong("memberId"))
            .contents(resultSet.getString("contents"))
            .createdDate(resultSet.getObject("createdDate", LocalDate.class))
            .createdAt(resultSet.getObject("createdAt", LocalDateTime.class))
            .likeCount(resultSet.getLong("likeCount"))
            .build();

    private static final RowMapper<DailyPostCount> DAILY_POST_COUNT_MAPPER = (ResultSet resultSet, int rowNum) -> new DailyPostCount(
            resultSet.getLong("memberId"),
            resultSet.getObject("createdDate", LocalDate.class),
            resultSet.getLong("cnt")
    );

    public List<DailyPostCount> groupByCreatedDate(DailyPostCountRequest request) {
        var sql = String.format("""
                 SELECT createdDate, memberId, count(id) as cnt
                 FROM %s
                 WHERE memberId = :memberId and createdDate between :firstDate and :lastDate
                 GROUP BY memberId, createdDate
                """, TABLE);
        var params = new BeanPropertySqlParameterSource(request);
        return namedParameterJdbcTemplate.query(sql, params, DAILY_POST_COUNT_MAPPER);
    }

    public Page<Post> findAllByMemberId(Long memberId, Pageable pageable) {
        var params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());

        var sql = String.format("""
                select *
                from %s
                where memberId = :memberId
                order by %s
                limit :size
                offset :offset
                """, TABLE, PageHelper.orderBy(pageable.getSort()));

        var posts = namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);

        return new PageImpl(posts, pageable, getCount(memberId));
    }

    public Optional<Post> findById(Long postId, Boolean requiredLock) {
        var sql = String.format("SELECT * FROM %s WHERE id = :postId", TABLE);
        if (requiredLock) {
            sql += "FOR UPDATE";
        }
        var params = new MapSqlParameterSource().addValue("postId", postId);
        var nullablePost = namedParameterJdbcTemplate.queryForObject(sql, params, ROW_MAPPER);

        return Optional.ofNullable(nullablePost);
    }

    private Long getCount(Long memberId) {
        var sql = String.format("""
                select count(id)
                FROM %s
                Where memberId = :memberId
                """, TABLE);
        var params = new MapSqlParameterSource()
                .addValue("memberId", memberId);
        return namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
    }

    public List<Post> findAllByMemberIdAndOrderByIdDecs(Long memberId, int size) {
        var sql = String.format("""
                select *
                FROM %s
                Where memberId = :memberId
                ORDER BY id desc
                Limit :size
                """, TABLE);
        var params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", size);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Post> findAllByInMemberIdAndOrderByIdDecs(List<Long> memberIds, int size) {
        if (memberIds.isEmpty()) {
            return List.of();
        }

        var sql = String.format("""
                select *
                FROM %s
                Where memberId In (:memberIds)
                ORDER BY id desc
                Limit :size
                """, TABLE);
        var params = new MapSqlParameterSource()
                .addValue("memberIds", memberIds)
                .addValue("size", size);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Post> findAllByInId(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        var sql = String.format("""
                select *
                FROM %s
                Where id in (:ids)
                """, TABLE);
        var params = new MapSqlParameterSource()
                .addValue("ids", ids);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Post> findAllByLessThanIdAndMemberIdAndOrderByIdDecs(Long id, Long memberId, int size) {
        var sql = String.format("""
                select *
                FROM %s
                Where memberId = :memberId and id < :id
                Order by id desc
                Limit :size
                """, TABLE);
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("memberId", memberId)
                .addValue("size", size);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Post> findAllByLessThanIdAndInMemberIdsAndOrderByIdDecs(Long id, List<Long> memberIds, int size) {
        if (memberIds.isEmpty()) {
            return List.of();
        }

        var sql = String.format("""
                select *
                FROM %s
                Where memberId In (:memberIds) and id < :id
                Order by id desc
                Limit :size
                """, TABLE);
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("memberIds", memberIds)
                .addValue("size", size);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }


    public Post save(Post post) {
        if (post.getId() == null) {
            return insert(post);
        }
        return update(post);
    }

    public void bulkInsert(List<Post> posts) {
        var sql = String.format("""
                INSERT INTO `%s` (memberId, contents, createdDate, createdAt)
                VALUES (:memberId, :contents, :createdDate, :createdAt)
                """, TABLE);

        SqlParameterSource[] params = posts
                .stream()
                .map(BeanPropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new);
        namedParameterJdbcTemplate.batchUpdate(sql, params);
    }

    private Post insert(Post post) {
        SimpleJdbcInsert jdbcinsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName(TABLE)
                .usingGeneratedKeyColumns("id");

        SqlParameterSource params = new BeanPropertySqlParameterSource(post);
        var id = jdbcinsert.executeAndReturnKey(params).longValue();

        return Post.builder()
                .id(id)
                .memberId(post.getMemberId())
                .contents(post.getContents())
                .createdDate(post.getCreatedDate())
                .createdAt(post.getCreatedAt())
                .build();
    }

    private Post update(Post post) {
        var sql = String.format("""
                UPDATE %s set
                memberId = :memberId,
                contents = :contents,
                createdDate = :createdDate,
                likeCount = :likeCount,
                createdAt = :createdAt
                where id = :id
                """, TABLE);
        SqlParameterSource params = new BeanPropertySqlParameterSource(post);
        namedParameterJdbcTemplate.update(sql, params);

        return post;
    }
}
