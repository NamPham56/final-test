import type { MediaChanges,MediaInfo } from './common'
export interface Course extends CourseForm { id:number;media:MediaInfo[];createdAt:string;updatedAt:string }
export interface CourseForm extends MediaChanges { courseCode:string;courseName:string;description?:string;price:number;startDate?:string;endDate?:string }
export interface CourseQuery { keyword?:string;fromDate?:string;toDate?:string;page:number;size:number }
