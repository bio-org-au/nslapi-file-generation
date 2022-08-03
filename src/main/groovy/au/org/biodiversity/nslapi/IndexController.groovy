package au.org.biodiversity.nslapi

import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Property
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces

import javax.annotation.security.PermitAll

/*
    This class is for the v1 NSL api endpoint.
    It contains routes for the v1 endpoint
* */

@Slf4j
@Controller("/")
class IndexController {
    @Property(name = "nslapi.search.e-limit")
    Integer eLimit

    @PermitAll
    @SuppressWarnings('GrMethodMayBeStatic')
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/prop")
    Map property() {
        log.debug("el: ${eLimit}")
        [status: "OK", exactLimit: "$eLimit"]
    }
}
