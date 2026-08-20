<template>
  <view class="page">
    <view class="avatar-section" @click="chooseAvatar">
      <view class="avatar-wrap">
        <image
          v-if="form.avatarUrl"
          class="avatar-img"
          :src="form.avatarUrl"
          mode="aspectFill"
        />
        <view v-else class="avatar-placeholder">{{ nicknameInitial }}</view>
      </view>
      <text class="avatar-hint">点击更换头像</text>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="form-label">昵称</text>
        <input
          class="form-input"
          v-model="form.nickname"
          placeholder="请输入昵称"
          maxlength="20"
        />
      </view>
      <view class="form-item">
        <text class="form-label">性别</text>
        <picker
          class="form-picker"
          :range="genderLabels"
          @change="onGenderChange"
          :value="genderIndex"
        >
          <text :class="['picker-text', { placeholder: form.gender === 0 }]">
            {{ genderLabels[genderIndex] || "请选择" }}
          </text>
        </picker>
      </view>
      <view class="form-item">
        <text class="form-label">出生日期</text>
        <picker
          class="form-picker"
          mode="date"
          :value="form.birthday || ''"
          start="1950-01-01"
          :end="todayStr"
          @change="onBirthdayChange"
        >
          <text :class="['picker-text', { placeholder: !form.birthday }]">
            {{ form.birthday || "请选择" }}
          </text>
        </picker>
      </view>
      <view class="form-item">
        <text class="form-label">身高</text>
        <view class="input-with-unit">
          <input
            class="form-input"
            v-model="form.height"
            type="digit"
            placeholder="请输入"
          />
          <text class="form-unit">cm</text>
        </view>
      </view>
      <view class="form-item">
        <text class="form-label">体重</text>
        <view class="input-with-unit">
          <input
            class="form-input"
            v-model="form.weight"
            type="digit"
            placeholder="请输入"
          />
          <text class="form-unit">kg</text>
        </view>
      </view>
      <view class="form-item">
        <text class="form-label">跑步等级</text>
        <picker
          class="form-picker"
          :range="levelLabels"
          @change="onLevelChange"
          :value="levelIndex"
        >
          <text :class="['picker-text', { placeholder: !form.runningLevel }]">
            {{ levelLabels[levelIndex] || "请选择" }}
          </text>
        </picker>
      </view>
      <view class="form-item">
        <text class="form-label">每周目标</text>
        <view class="input-with-unit">
          <input
            class="form-input"
            v-model="form.runningGoal"
            type="number"
            placeholder="请输入"
          />
          <text class="form-unit">次/周</text>
        </view>
      </view>
    </view>

    <view class="save-btn" hover-class="save-btn-hover" @click="saveProfile">
      <text class="save-text">保存修改</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted } from "vue";
import { put, get } from "@/utils/request";

const genderLabels = ["未知", "男", "女"];
const levelLabels = ["新手入门", "进阶跑者", "资深跑者"];
const levelValues = ["beginner", "intermediate", "advanced"];

const today = new Date();
const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

const form = reactive({
  nickname: "",
  avatarUrl: "",
  gender: 0,
  birthday: "",
  height: "",
  weight: "",
  runningLevel: "",
  runningGoal: "",
});

const genderIndex = computed(() => form.gender || 0);
const levelIndex = computed(() => {
  const idx = levelValues.indexOf(form.runningLevel);
  return idx >= 0 ? idx : 0;
});

const nicknameInitial = computed(
  () => form.nickname?.charAt(0) || "跑"
);

const onGenderChange = (e: any) => {
  form.gender = Number(e.detail.value);
};

const onBirthdayChange = (e: any) => {
  form.birthday = e.detail.value;
};

const onLevelChange = (e: any) => {
  form.runningLevel = levelValues[Number(e.detail.value)];
};

const chooseAvatar = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ["compressed"],
    sourceType: ["album", "camera"],
    success: (res) => {
      form.avatarUrl = res.tempFilePaths[0];
    },
  });
};

const loadProfile = async () => {
  try {
    const data = await get<any>("/api/user/profile");
    if (data) {
      form.nickname = data.nickname || "";
      form.avatarUrl = data.avatarUrl || "";
      form.gender = data.gender || 0;
      form.birthday = data.birthday || "";
      form.height = data.height ? String(data.height) : "";
      form.weight = data.weight ? String(data.weight) : "";
      form.runningLevel = data.runningLevel || "beginner";
      form.runningGoal = data.runningGoal ? String(data.runningGoal) : "";
    }
  } catch (error) {
    console.error("加载个人资料失败", error);
  }
};

const saveProfile = async () => {
  if (!form.nickname.trim()) {
    uni.showToast({ title: "请输入昵称", icon: "none" });
    return;
  }

  const payload: Record<string, any> = {
    nickname: form.nickname.trim(),
    avatarUrl: form.avatarUrl,
    gender: form.gender,
    birthday: form.birthday || null,
    height: form.height ? parseFloat(form.height) : null,
    weight: form.weight ? parseFloat(form.weight) : null,
    runningLevel: form.runningLevel || "beginner",
    runningGoal: form.runningGoal ? parseInt(form.runningGoal) : null,
  };

  try {
    await put("/api/user/profile", payload);
    uni.showToast({ title: "保存成功", icon: "success" });
  } catch (error) {
    console.error("保存个人资料失败", error);
    uni.showToast({ title: "保存失败，请重试", icon: "none" });
  }
};

onMounted(() => {
  loadProfile();
});
</script>

<style lang="scss">
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #e6f7ff 100%);
  padding: 32rpx;
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
}

.avatar-wrap {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  border: 4rpx solid $rw-primary;
  overflow: hidden;
  background-color: $rw-primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
}

.avatar-placeholder {
  font-size: 56rpx;
  font-weight: 600;
  color: $rw-primary;
}

.avatar-hint {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: $rw-text-placeholder;
}

.form-card {
  background: $rw-card-bg;
  border: $rw-card-border;
  border-radius: $rw-card-radius;
  box-shadow: $rw-shadow-card;
  overflow: hidden;
}

.form-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid $rw-divider;

  &:last-child {
    border-bottom: none;
  }
}

.form-label {
  font-size: 28rpx;
  color: $rw-text-primary;
  font-weight: 500;
  flex-shrink: 0;
  width: 160rpx;
}

.form-input {
  flex: 1;
  text-align: right;
  font-size: 28rpx;
  color: $rw-text-primary;
}

.input-with-unit {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.input-with-unit .form-input {
  width: 120rpx;
  text-align: right;
}

.form-unit {
  font-size: 24rpx;
  color: $rw-text-placeholder;
  margin-left: 8rpx;
  flex-shrink: 0;
}

.form-picker {
  flex: 1;
  text-align: right;
}

.picker-text {
  font-size: 28rpx;
  color: $rw-text-primary;

  &.placeholder {
    color: $rw-text-placeholder;
  }
}

.save-btn {
  margin-top: 64rpx;
  height: 88rpx;
  border-radius: 48rpx;
  background: $rw-gradient-primary;
  box-shadow: $rw-shadow-btn;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: $rw-transition-smooth;
}

.save-btn-hover {
  transform: scale(0.95);
  box-shadow: $rw-shadow-btn-pressed;
}

.save-text {
  font-size: 28rpx;
  font-weight: 500;
  color: #ffffff;
}
</style>