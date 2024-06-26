package kpring.auth.service

import io.jsonwebtoken.security.Keys
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotHaveLength
import io.mockk.coEvery
import io.mockk.mockk
import kpring.auth.error.AuthErrorCode
import kpring.auth.repository.ExpireTokenRepository
import kpring.auth.util.toToken
import kpring.core.auth.dto.request.CreateTokenRequest
import kpring.core.auth.enums.TokenType
import kpring.core.global.exception.ServiceException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId

class TokenServiceTest : BehaviorSpec({
  val tokenRepository: ExpireTokenRepository = mockk()
  val accessDuration = 100000 // 100s
  val refreshDuration = 1000000 // 1000s
  val secretKey = "testsecretkey-dfasdfasdfasdfasdfasdfsadfasdfasdfasdfasdf"
  val tokenService =
    TokenService(
      accessDuration = accessDuration,
      refreshDuration = refreshDuration,
      secretKey = secretKey,
      tokenRepository,
    )

  beforeTest {
    tokenService.init()
  }

  Given("유저의 id와 유저 닉네임이 주어질 때") {
    val request =
      CreateTokenRequest(
        id = "testUserId",
        nickname = "test user",
      )

    When("토큰을 생성하면") {
      then("엑세스 토큰과 리프레시 토큰이 생성된다.").config(invocations = 100) {
        val time = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val response = tokenService.createToken(request)
        response.apply {
          accessToken shouldNotHaveLength 0
          accessExpireAt shouldBeBefore time.plusSeconds((accessDuration / 1000).toLong() + 1)
          refreshToken shouldNotHaveLength 0
          refreshExpireAt shouldBeBefore time.plusSeconds((refreshDuration / 1000).toLong() + 1)
        }
      }
    }
  }

  Given("토큰이 주어졌을 때") {
    val tokenInfo =
      tokenService.createToken(
        CreateTokenRequest(id = "test", nickname = "test nick"),
      )

    When("토큰을 재생성하면") {
      coEvery { tokenRepository.isExpired(any()) } returns false
      val newTokenInfo = tokenService.reCreateAccessToken(tokenInfo.refreshToken)
      Then("새로운 엑세스 토큰을 반환한다.") {
        newTokenInfo.accessToken shouldNotBe tokenInfo.accessToken
      }
    }

    When("타입이 Refresh가 아니라면") {
      then("토큰 재생성시 예외가 발생한다.") {
        coEvery { tokenRepository.isExpired(any()) } returns false
        shouldThrow<IllegalArgumentException> {
          tokenService.reCreateAccessToken(tokenInfo.accessToken)
        }
      }
    }

    When("토큰이 유효하지 않다면") {
      val otherKey =
        Keys.hmacShaKeyFor(
          "invalid jwt secret key testtesttesttesttest".toByteArray(
            StandardCharsets.UTF_8,
          ),
        )!!
      coEvery { tokenRepository.isExpired(any()) } returns false
      val invalidTokenInfo =
        CreateTokenRequest("invalid", "invalid")
          .toToken(TokenType.REFRESH, otherKey, 100000)
      then("토큰 검증시 예외가 발생한다.") {
        val ex =
          shouldThrow<ServiceException> {
            tokenService.checkToken(invalidTokenInfo.token)
          }
        ex.errorCode shouldBe AuthErrorCode.TOKEN_NOT_VALID
      }

      then("토큰 재생성시 예외가 발생한다.") {
        val ex =
          shouldThrow<ServiceException> {
            tokenService.reCreateAccessToken(invalidTokenInfo.token)
          }
        ex.errorCode shouldBe AuthErrorCode.TOKEN_NOT_VALID
      }
    }

    When("만료된 토큰이라면") {
      coEvery { tokenRepository.isExpired(any()) } returns true
      then("토큰 검증시 토큰 만료 서비스 예외가 발생한다.") {
        val ex =
          shouldThrow<ServiceException> {
            tokenService.checkToken(tokenInfo.accessToken)
          }
        ex.errorCode shouldBe AuthErrorCode.TOKEN_EXPIRED
      }
    }

    When("만료 처리가 되었다면") {
      val validKey =
        Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8))
          ?: throw IllegalStateException("토큰을 발급하기 위한 key가 적절하지 않습니다.")
      val expiredToken =
        CreateTokenRequest("test", "nick").toToken(TokenType.ACCESS, validKey, -1).token
      coEvery { tokenRepository.isExpired(any()) } returns true

      then("토큰 검증시 토큰 만료 서비스 에러를 반환한다.") {
        val ex =
          shouldThrow<ServiceException> {
            tokenService.checkToken(expiredToken)
          }
        ex.errorCode.id() shouldBe "AUTH_0002"
        ex.errorCode.message() shouldBe "토큰이 만료되었습니다."
      }

      then("토큰 재성시 토큰 만료 서비스 예외가 발생한다.") {
        val ex =
          shouldThrow<ServiceException> {
            tokenService.reCreateAccessToken(expiredToken)
          }
        ex.errorCode shouldBe AuthErrorCode.TOKEN_EXPIRED
      }
    }
  }
})
