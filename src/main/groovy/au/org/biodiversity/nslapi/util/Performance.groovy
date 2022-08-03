package au.org.biodiversity.nslapi.util

import groovy.time.TimeCategory
import groovy.util.logging.Slf4j

@Slf4j
class Performance {
    static void printTime(Date start, Integer num) {
        Date stop = new Date()
        log.debug("${num}: ${TimeCategory.minus( stop, start )}")
    }
}
