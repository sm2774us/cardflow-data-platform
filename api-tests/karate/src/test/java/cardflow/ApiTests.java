package cardflow;

import com.intuit.karate.junit5.Karate;

class ApiTests {
    @Karate.Test
    Karate all() {
        return Karate.run("classpath:cardflow").relativeTo(getClass());
    }
}
