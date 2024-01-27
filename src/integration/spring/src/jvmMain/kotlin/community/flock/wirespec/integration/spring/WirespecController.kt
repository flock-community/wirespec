package community.flock.wirespec.integration.spring.annotations

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
@ResponseBody
annotation class WirespecController {
}