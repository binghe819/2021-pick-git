package com.woowacourse.pickgit.unit.post.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.woowacourse.pickgit.common.factory.PostBuilder;
import com.woowacourse.pickgit.common.factory.UserFactory;
import com.woowacourse.pickgit.config.JpaTestConfiguration;
import com.woowacourse.pickgit.post.domain.Post;
import com.woowacourse.pickgit.post.domain.PostRepository;
import com.woowacourse.pickgit.post.domain.comment.Comment;
import com.woowacourse.pickgit.tag.domain.Tag;
import com.woowacourse.pickgit.tag.domain.TagRepository;
import com.woowacourse.pickgit.user.domain.User;
import com.woowacourse.pickgit.user.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;

@Import(JpaTestConfiguration.class)
@DataJpaTest
class PostRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private TagRepository tagRepository;

    @DisplayName("게시글을 저장한다.")
    @Test
    void save_SavedPost_Success() {
        // given
        User savedTestUser = userRepository.save(
            UserFactory.user("testUser")
        );

        Post post = new PostBuilder()
            .content("test")
            .user(savedTestUser)
            .build();

        // when
        Post savedPost = postRepository.save(post);
        flushAndClear();

        Post actual = postRepository.findById(savedPost.getId())
            .orElseThrow(IllegalArgumentException::new);

        // then
        assertThat(actual.getId()).isEqualTo(savedPost.getId());
    }

    @DisplayName("게시글을 저장하면 자동으로 생성 날짜가 주입된다.")
    @Test
    void save_SavedPostWithCreatedDate_Success() {
        // given
        User savedTestUser = userRepository.save(
            UserFactory.user("testUser")
        );

        Post post = new PostBuilder()
            .content("test")
            .user(savedTestUser)
            .build();

        // when
        Post savedPost = postRepository.save(post);

        // then
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getCreatedAt()).isBefore(LocalDateTime.now());
    }

    @DisplayName("Post을 저장할 때 PostTag도 함께 영속화된다.")
    @Test
    void save_WhenSavingPost_TagSavedTogether() {
        //given
        User testUser = UserFactory.user("testUser");
        User savedTestUser = userRepository.save(testUser);

        Post post = new PostBuilder()
            .content("testContent")
            .githubRepoUrl("https://github.com/bperhaps")
            .user(savedTestUser)
            .build();
        Post savedPost = postRepository.save(post);

        Tag tag1 = new Tag("tag1");
        Tag tag2 = new Tag("tag2");

        tagRepository.save(tag1);
        tagRepository.save(tag2);

        // when
        post.addTags(List.of(tag1, tag2));
        flushAndClear();

        // then
        Post findPost = postRepository.findById(savedPost.getId())
            .orElse(null);

        assertThat(findPost).isNotNull();
        assertThat(findPost.getTags()).hasSize(2);
    }

    @DisplayName("Post를 저장할 때 Tag는 함께 영속화되지 않는다. (태그가 존재하지 않을 경우 예외가 발생한다)")
    @Test
    void save_WhenSavingPost_TagNotSavedTogether() {
        // given
        Post post = new PostBuilder()
            .content("abc")
            .build();
        List<Tag> tags = Arrays.asList(new Tag("tag1"), new Tag("tag2"));

        // when, then
        post.addTags(tags);
        assertThatThrownBy(() -> postRepository.save(post))
            .isInstanceOf(InvalidDataAccessApiUsageException.class);
    }

    @DisplayName("Post에 Comment를 추가하면 Comment가 자동 영속화된다.")
    @Test
    void addComment_WhenSavingPost_CommentSavedTogether() {
        // given
        User testUser = UserFactory.user("testUser");
        User savedTestUser = userRepository.save(testUser);

        Post post = new PostBuilder()
            .content("testContent")
            .githubRepoUrl("https://github.com/bperhaps")
            .user(savedTestUser)
            .build();

        // when
        Comment comment = new Comment("test comment");
        post.addComment(comment);

        postRepository.save(post);
        flushAndClear();

        // then
        Post findPost = postRepository.findById(post.getId())
            .orElseThrow(IllegalArgumentException::new);

        List<Comment> comments = findPost.getComments();
        assertThat(comments).hasSize(1);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }
}