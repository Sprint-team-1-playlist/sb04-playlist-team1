package com.codeit.playlist.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.playlist.domain.auth.service.AuthService;
import com.codeit.playlist.domain.base.SortDirection;
import com.codeit.playlist.domain.user.dto.data.UserDto;
import com.codeit.playlist.domain.user.dto.request.ChangePasswordRequest;
import com.codeit.playlist.domain.user.dto.request.UserCreateRequest;
import com.codeit.playlist.domain.user.dto.request.UserLockUpdateRequest;
import com.codeit.playlist.domain.user.dto.request.UserRoleUpdateRequest;
import com.codeit.playlist.domain.user.dto.request.UserUpdateRequest;
import com.codeit.playlist.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.playlist.domain.user.entity.Role;
import com.codeit.playlist.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Mock
  UserService userService;

  @Mock
  AuthService authService;

  MockMvc mockMvc;

  ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    UserController controller =
        new UserController(userService, authService);

    mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .build();
  }

  @Test
  @DisplayName("POST /api/users - 사용자 등록 성공")
  void registerSuccess() throws Exception {
    UserCreateRequest request =
        new UserCreateRequest("홍길동", "test@test.com", "password");

    UUID userId = UUID.randomUUID();

    UserDto response = new UserDto(
        userId,
        LocalDateTime.now(),
        "test@test.com",
        "홍길동",
        null,
        Role.USER,
        false
    );

    org.mockito.BDDMockito.given(userService.registerUser(any()))
        .willReturn(response);

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  @DisplayName("GET /api/users/{id} - 사용자 조회 성공")
  void findSuccess() throws Exception {
    UUID userId = UUID.randomUUID();

    UserDto response = new UserDto(
        userId,
        LocalDateTime.now(),
        "test@test.com",
        "홍길동",
        null,
        Role.USER,
        false
    );

    org.mockito.BDDMockito.given(userService.find(userId))
        .willReturn(response);

    mockMvc.perform(get("/api/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()));
  }

  @Test
  @WithMockUser
  @DisplayName("PATCH /api/users/{id} - 사용자 수정 성공")
  void updateUserSuccess() throws Exception {
    UUID userId = UUID.randomUUID();

    UserUpdateRequest request =
        new UserUpdateRequest("변경이름");

    UserDto response = new UserDto(
        userId,
        LocalDateTime.now(),
        "test@test.com",
        "변경이름",
        null,
        Role.USER,
        false
    );

    MockMultipartFile requestPart =
        new MockMultipartFile(
            "request",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "profile.png",
            MediaType.IMAGE_PNG_VALUE,
            "image-data".getBytes(StandardCharsets.UTF_8)
        );

    org.mockito.BDDMockito.given(
        userService.updateUser(eq(userId), any(), any(), any())
    ).willReturn(response);

    mockMvc.perform(
            multipart("/api/users/{userId}", userId)
                .file(requestPart)
                .file(image)
                .with(req -> {
                  req.setMethod("PATCH");
                  return req;
                })
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("변경이름"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/users - 관리자 사용자 검색")
  void searchUsersAdminSuccess() throws Exception {
    CursorResponseUserDto response =
        new CursorResponseUserDto(
            List.of(),
            null,
            null,
            false,
            0L,
            "createdAt",
            SortDirection.ASCENDING
        );

    org.mockito.BDDMockito.given(
        userService.findUserList(
            any(), any(), any(), any(), any(), anyInt(), any(), any()
        )
    ).willReturn(response);

    mockMvc.perform(get("/api/users")
            .param("limit", "10")
            .param("sortDirection", SortDirection.ASCENDING.name()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  @DisplayName("PATCH /api/users/{id}/password - 비밀번호 변경")
  void changePasswordSuccess() throws Exception {
    UUID userId = UUID.randomUUID();

    ChangePasswordRequest request =
        new ChangePasswordRequest("newPw");

    mockMvc.perform(
            patch("/api/users/{userId}/password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH /api/users/{id}/role - 관리자 권한 변경")
  void updateRoleSuccess() throws Exception {
    UUID userId = UUID.randomUUID();

    UserRoleUpdateRequest request =
        new UserRoleUpdateRequest(Role.ADMIN);

    mockMvc.perform(
            patch("/api/users/{userId}/role", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PATCH /api/users/{id}/locked - 사용자 잠금")
  void updateUserLockedSuccess() throws Exception {
    UUID userId = UUID.randomUUID();

    UserLockUpdateRequest request =
        new UserLockUpdateRequest(true);

    mockMvc.perform(
            patch("/api/users/{userId}/locked", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk());
  }
}