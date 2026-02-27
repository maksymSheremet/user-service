package my.code.userservice.mapper;

import my.code.userservice.dto.event.UserRegisteredEvent;
import my.code.userservice.dto.response.UserProfileResponse;
import my.code.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    UserProfileResponse toResponse(User user);

    @Mapping(source = "userId", target = "id")
    User fromEvent(UserRegisteredEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "onboardingCompleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromEvent(UserRegisteredEvent event, @MappingTarget User user);
}
