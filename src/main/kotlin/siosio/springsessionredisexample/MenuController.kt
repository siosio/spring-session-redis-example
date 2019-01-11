package siosio.springsessionredisexample

import org.springframework.security.core.annotation.*
import org.springframework.stereotype.*
import org.springframework.web.bind.annotation.*
import siosio.springsessionredisexample.config.*

@Controller
@RequestMapping("/menu")
class MenuController {

    @GetMapping
    fun menu(@AuthenticationPrincipal user: SampleUser): String {
        println("user = ${user}")
        return "menu"
    }
}