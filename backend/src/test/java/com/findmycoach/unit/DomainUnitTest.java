package com.findmycoach.unit;

import com.findmycoach.common.Slugs;
import com.findmycoach.common.jpa.StringListConverter;
import com.findmycoach.trainer.Trainer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainUnitTest {

    @Test
    void ratingAverageIncludesBaseline() {
        Trainer t = new Trainer();
        t.setRatingTotal(4.9 * 184);
        t.setReviewCount(184);
        t.recalcRating();
        assertThat(t.getRating()).isEqualTo(4.9);
        t.addRating(1);
        assertThat(t.getReviewCount()).isEqualTo(185);
        assertThat(t.getRating()).isEqualTo(4.9);
        t.removeRating(1);
        assertThat(t.getReviewCount()).isEqualTo(184);
        Trainer empty = new Trainer();
        empty.removeRating(5);
        assertThat(empty.getReviewCount()).isZero();
        empty.addRating(4);
        assertThat(empty.getRating()).isEqualTo(4.0);
    }

    @Test
    void slugify() {
        assertThat(Slugs.slugify("Zoë  O'Brien")).isEqualTo("zoe-o-brien");
        assertThat(Slugs.slugify("!!!")).isEqualTo("item");
    }

    @Test
    void stringListConverterRoundTrip() {
        StringListConverter c = new StringListConverter();
        assertThat(c.convertToDatabaseColumn(List.of("Serve & Volley", " Match Play "))).isEqualTo("Serve & Volley|Match Play");
        assertThat(c.convertToEntityAttribute("a|b||c")).containsExactly("a", "b", "c");
        assertThat(c.convertToEntityAttribute(null)).isEmpty();
        assertThat(c.convertToDatabaseColumn(List.of("a|b"))).isEqualTo("a/b");
    }
}
