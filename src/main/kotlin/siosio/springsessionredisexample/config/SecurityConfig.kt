package siosio.springsessionredisexample.config

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.*
import com.fasterxml.jackson.module.kotlin.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.data.redis.serializer.*
import org.springframework.security.config.annotation.authentication.builders.*
import org.springframework.security.config.annotation.web.builders.*
import org.springframework.security.config.annotation.web.configuration.*
import org.springframework.security.core.*
import org.springframework.security.core.authority.*
import org.springframework.security.core.userdetails.*
import org.springframework.security.crypto.bcrypt.*
import org.springframework.security.crypto.password.*
import org.springframework.security.jackson2.*


@Configuration
class SecurityConfig : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http.authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .formLogin().defaultSuccessUrl("/menu", true).permitAll()
                .and()
                .logout().invalidateHttpSession(true)
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(UserDetailsService {
            SampleUser("test", passwordEncoder().encode("password"), listOf(SimpleGrantedAuthority("ROLE_USER")), "テストユーザ")
        })
                .passwordEncoder(passwordEncoder())
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @Qualifier("springSessionDefaultRedisSerializer")
    fun redisSerializer(): RedisSerializer<Any> {
        return Jackson2JsonRedisSerializer(Any::class.java).apply {
            val objectMapper = ObjectMapper()
            objectMapper.registerModules(SecurityJackson2Modules.getModules(this.javaClass.classLoader))
            objectMapper.addMixIn(SampleUser::class.java, SampleUserMixin::class.java)
            setObjectMapper(objectMapper)
        }
    }
}

data class SampleUser(private val username: String,
                      private val password: String,
                      private val authorities: List<GrantedAuthority>,
                      val loginUserName: String) : UserDetails {

    override fun getAuthorities(): List<GrantedAuthority> = authorities

    override fun isEnabled(): Boolean = true

    override fun getUsername(): String = username

    override fun isCredentialsNonExpired(): Boolean = true

    override fun getPassword(): String = password

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true
}

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
@JsonDeserialize(using = SampleUserDeserializer::class)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes
internal abstract class SampleUserMixin

class SampleUserDeserializer : JsonDeserializer<SampleUser>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): SampleUser {
        val mapper = jp.codec as ObjectMapper
        val jsonNode: JsonNode = mapper.readTree(jp)
        val authorities = mapper.convertValue<List<SimpleGrantedAuthority>>(jsonNode.get("authorities"))

        val password = jsonNode.readText("password")
        return SampleUser(
                jsonNode.readText("username"),
                password,
                authorities.toList(),
                jsonNode.readText("loginUserName")
        )
    }

    fun JsonNode.readText(field: String, defaultValue: String = ""): String {
        return when {
            has(field) -> get(field).asText(defaultValue)
            else -> defaultValue
        }
    }
}
