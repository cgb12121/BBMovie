package com.bbmovie.watchlist.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/test")
class TestController {

    @RequestMapping("/hello")
    fun test(): ResponseEntity<String> {
        return  ResponseEntity.status(HttpStatus.OK).body("Hello");
    }
}