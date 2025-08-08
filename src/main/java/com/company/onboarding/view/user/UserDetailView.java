package com.company.onboarding.view.user;

import com.company.onboarding.entity.OnboardingStatus;
import com.company.onboarding.entity.Step;
import com.company.onboarding.entity.User;
import com.company.onboarding.entity.UserStep;
import com.company.onboarding.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

@Route(value = "users/:id", layout = MainView.class)
@ViewController(id = "User.detail")
@ViewDescriptor(path = "user-detail-view.xml")
@EditedEntityContainer("userDc")
public class UserDetailView extends StandardDetailView<User> {
    @Autowired
    private DataManager dataManager;

    @Autowired
    private UiComponents uiComponents;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionPropertyContainer<UserStep> stepsDc;

    @ViewComponent
    private TypedTextField<String> usernameField;
    @ViewComponent
    private PasswordField passwordField;
    @ViewComponent
    private PasswordField confirmPasswordField;
    @ViewComponent
    private ComboBox<String> timeZoneField;
    @ViewComponent
    private MessageBundle messageBundle;
    @Autowired
    private Notifications notifications;

    @Autowired
    private EntityStates entityStates;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Supply(to = "stepsDataGrid.completed", subject = "renderer")
    private Renderer<UserStep> stepsDataGridCompletedRenderer() {
        return new ComponentRenderer<>(userStep -> {
            com.vaadin.flow.component.checkbox.Checkbox checkbox = new com.vaadin.flow.component.checkbox.Checkbox();
            checkbox.setClassName("centered-checkbox");
            checkbox.setValue(userStep.getCompletedDate() != null);
            checkbox.addValueChangeListener(e -> { 
                if (e.getValue()) {
                    userStep.setCompletedDate(java.time.LocalDate.now());
                } else {
                    userStep.setCompletedDate(null);
                }
                // Cập nhật grid để hiển thị thay đổi ngay lập tức
                stepsDc.getItems().forEach(item -> {
                    if (item.equals(userStep)) {
                        item.setCompletedDate(userStep.getCompletedDate());
                    }
                });
            });
            return checkbox; 
        });
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        timeZoneField.setItems(List.of(TimeZone.getAvailableIDs()));
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<User> event) {
        usernameField.setReadOnly(false);
        passwordField.setVisible(true);
        confirmPasswordField.setVisible(true);
        User user = event.getEntity();
        user.setOnboardingStatus(OnboardingStatus.NOT_STARTED);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            usernameField.focus();
        }
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        if (entityStates.isNew(getEditedEntity())
                && !Objects.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
            event.getErrors().add(messageBundle.getMessage("passwordsDoNotMatch"));
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            getEditedEntity().setPassword(passwordEncoder.encode(passwordField.getValue()));

            notifications.create(messageBundle.getMessage("noAssignedRolesNotification"))
                    .withType(Notifications.Type.WARNING)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
        }
    }


    @Subscribe("generateButton")
    public void onGenerateButtonClick(final ClickEvent<Button> event) {
        User user = getEditedEntity();

        if (user.getJoiningDate() == null) {
            notifications.create("Cannot generate steps for user without 'Joining date'")
                    .show();
            return;
        }

        // Xóa các bước hiện có
        stepsDc.getMutableItems().clear();

        // Tạo các bước mẫu như trong hình
        createSampleSteps(user);
    }

    private void createSampleSteps(User user) {
        // Tạo các bước như trong hình mẫu
        java.time.LocalDate baseDate = java.time.LocalDate.of(2023, 6, 15);

        // Step 1: Safety briefing (completed)
        Step step1 = getOrCreateStep("Safety briefing", 0, 1);
        UserStep userStep1 = dataContext.create(UserStep.class);
        userStep1.setUser(user);
        userStep1.setStep(step1);
        userStep1.setDueDate(baseDate);
        userStep1.setCompletedDate(baseDate); // Đã hoàn thành
        userStep1.setSortValue(1);
        stepsDc.getMutableItems().add(userStep1);

        // Step 2: Fill in profile
        Step step2 = getOrCreateStep("Fill in profile", 0, 2);
        UserStep userStep2 = dataContext.create(UserStep.class);
        userStep2.setUser(user);
        userStep2.setStep(step2);
        userStep2.setDueDate(baseDate);
        userStep2.setSortValue(2);
        stepsDc.getMutableItems().add(userStep2);

        // Step 3: Check all functions
        Step step3 = getOrCreateStep("Check all functions", 1, 3);
        UserStep userStep3 = dataContext.create(UserStep.class);
        userStep3.setUser(user);
        userStep3.setStep(step3);
        userStep3.setDueDate(baseDate.plusDays(1)); // 16/06/2023
        userStep3.setSortValue(3);
        stepsDc.getMutableItems().add(userStep3);

        // Step 4: Information security training
        Step step4 = getOrCreateStep("Information security training", 2, 4);
        UserStep userStep4 = dataContext.create(UserStep.class);
        userStep4.setUser(user);
        userStep4.setStep(step4);
        userStep4.setDueDate(baseDate.plusDays(2)); // 17/06/2023
        userStep4.setSortValue(4);
        stepsDc.getMutableItems().add(userStep4);
    }

    private Step getOrCreateStep(String name, int duration, int sortValue) {
        // Tìm step nếu đã tồn tại
        List<Step> existingSteps = dataManager.load(Step.class)
                .query("select s from Step s where s.name = :name")
                .parameter("name", name)
                .list();

        if (!existingSteps.isEmpty()) {
            return existingSteps.get(0);
        }

        // Tạo step mới nếu chưa tồn tại
        Step step = dataContext.create(Step.class);
        step.setName(name);
        step.setDuration(duration);
        step.setSortValue(sortValue);
        return dataManager.save(step);
    }
}