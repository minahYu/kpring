package kpring.user.controller

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.junit5.MockKExtension
import kpring.test.restdoc.dsl.restDoc
import kpring.user.dto.request.AddFriendRequest
import kpring.user.dto.request.CreateUserRequest
import kpring.user.dto.result.CreateUserResponse
import kpring.user.service.UserService
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.ManualRestDocumentation
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.event.annotation.AfterTestMethod
import org.springframework.test.context.event.annotation.BeforeTestMethod
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@WebMvcTest(controllers = [UserController::class])
@ExtendWith(value = [MockKExtension::class])
class UserControllerTest(
    webContext: WebApplicationContext,
    val restDocument: ManualRestDocumentation = ManualRestDocumentation(),
    @MockkBean val userService: UserService,
) : DescribeSpec(
    {

        val webTestClient = MockMvcWebTestClient.bindToApplicationContext(webContext)
            .configureClient()
            .filter(
                WebTestClientRestDocumentation.documentationConfiguration(restDocument)
                    .operationPreprocessors()
                    .withRequestDefaults(prettyPrint())
                    .withResponseDefaults(prettyPrint())
            )
            .build()

        beforeSpec { restDocument.beforeTest(this.javaClass, "user controller") }

        afterSpec { restDocument.afterTest() }

        describe("회원가입 API") {

            it("회원가입 성공") {
                // given
                val request = CreateUserRequest.builder().email("test@email.com").build()
                val response = CreateUserResponse.builder().build()
                every { userService.createUser(request) } returns response

                // when
                val result = webTestClient.post()
                    .uri("/api/v1/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()

                // then
                val docsRoot = result
                    .expectStatus().isOk
                    .expectBody()

                // docs
                docsRoot
                    .restDoc("createUser200", "회원가입 API")
                    {
                        request {
                            header {
                                "Content-Type" mean "application/json"
                            }
                            body {
                                "email" type String mean "이메일"
                            }
                        }
                    }
            }
        }
    }
) {


//    @Test
//    @WithMockUser(username = "testUser", roles = ["USER"])
//    fun `친구추가 성공케이스`() {
//        val userId = 1L;
//        val friendsRequestDto = AddFriendRequest(friendId = 2L)
//
//        webTestClient.post()
//            .uri("/api/v1/user/{userId}/friend/{friendId}", userId, 2)
//            .contentType(MediaType.APPLICATION_JSON)
//            .bodyValue(friendsRequestDto)
//            .exchange()
//            .expectStatus().isOk
//            .expectBody()
//    }
//
//    @Test
//    @WithMockUser(username = "testUser", roles = ["USER"])
//    fun `친구추가_실패케이스`() {
//        val userId = -1L // 유효하지 않은 사용자 아이디
//        val friendsRequestDto = AddFriendRequest(friendId = 2L)
//
//        webTestClient.post()
//            .uri("/api/v1/user/{userId}/friend/{friendId}", userId, 2)
//            .contentType(MediaType.APPLICATION_JSON)
//            .bodyValue(friendsRequestDto)
//            .exchange()
//            .expectStatus().isBadRequest
//            .expectBody()
//    }
}
