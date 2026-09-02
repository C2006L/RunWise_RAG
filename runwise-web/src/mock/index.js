// mock 总开关（工程计划 6.2）：组件层无感知，api 层据此分流 mock / axios
// VITE_USE_MOCK 显式为 'false' 时走真实接口，其余情况（缺省 / 'true'）走 mock
export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== "false";

export * as auth from "./user";
export * as checkin from "./checkin";
export * as qa from "./qa";
export * as stats from "./stats";
export * as plan from "./plan";
export * as injury from "./injury";
